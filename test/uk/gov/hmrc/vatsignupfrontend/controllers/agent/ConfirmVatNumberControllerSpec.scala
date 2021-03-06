/*
 * Copyright 2019 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.vatsignupfrontend.controllers.agent

import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.Status
import play.api.http.Status.INTERNAL_SERVER_ERROR
import play.api.libs.json.Json
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.http.InternalServerException
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.vatsignupfrontend.SessionKeys
import uk.gov.hmrc.vatsignupfrontend.config.mocks.MockControllerComponents
import uk.gov.hmrc.vatsignupfrontend.helpers.TestConstants._
import uk.gov.hmrc.vatsignupfrontend.services.StoreVatNumberService._
import uk.gov.hmrc.vatsignupfrontend.models.MigratableDates
import uk.gov.hmrc.vatsignupfrontend.services.mocks.MockStoreVatNumberService

import scala.concurrent.Future

class ConfirmVatNumberControllerSpec extends UnitSpec with GuiceOneAppPerSuite with MockControllerComponents
  with MockStoreVatNumberService {

  object TestConfirmVatNumberController extends ConfirmVatNumberController(mockControllerComponents, mockStoreVatNumberService)

  lazy val testGetRequest = FakeRequest("GET", "/confirm-vat-number")

  lazy val testPostRequest: FakeRequest[AnyContentAsEmpty.type] =
    FakeRequest("POST", "/confirm-vat-number")

  "Calling the show action of the Confirm Vat Number controller" when {
    "there is a vrn in the session" should {
      "go to the Confirm Vat number page" in {
        mockAuthRetrieveAgentEnrolment()
        val request = testGetRequest.withSession(SessionKeys.vatNumberKey -> testVatNumber)

        val result = TestConfirmVatNumberController.show(request)
        status(result) shouldBe Status.OK
        contentType(result) shouldBe Some("text/html")
        charset(result) shouldBe Some("utf-8")
      }
    }

    "there isn't a vrn in the session" should {
      "redirect to Capture Vat number page" in {
        mockAuthRetrieveAgentEnrolment()

        val result = TestConfirmVatNumberController.show(testGetRequest)
        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(routes.CaptureVatNumberController.show().url)
      }
    }
  }

  "Calling the submit action of the Confirm Vat Number controller" when {

    "vat number is in session but it is invalid" should {
      "go to invalid vat number page" in {
        mockAuthRetrieveAgentEnrolment()

        val request = testPostRequest.withSession(SessionKeys.vatNumberKey -> testInvalidVatNumber)
        val result = TestConfirmVatNumberController.submit(request)

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) should contain(routes.CouldNotConfirmVatNumberController.show().url)

        await(result).session(request).get(SessionKeys.vatNumberKey) shouldBe None
      }
    }

    "vat number is in session and store vat is successful" should {
      "go to the business entity type page" in {
        mockAuthRetrieveAgentEnrolment()
        mockStoreVatNumberDelegated(vatNumber = testVatNumber)(Future.successful(Right(VatNumberStored(isOverseas = false, isDirectDebit = false))))

        val result = TestConfirmVatNumberController.submit(testPostRequest.withSession(SessionKeys.vatNumberKey -> testVatNumber))
        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) should contain(routes.CaptureBusinessEntityController.show().url)
        session(result) get SessionKeys.hasDirectDebitKey should contain("false")
      }
    }

    "overseas vat number is in session and store vat is successful" should {
      "go to the overseas resolver controller" in {
        mockAuthRetrieveAgentEnrolment()
        mockStoreVatNumberDelegated(vatNumber = testVatNumber)(Future.successful(Right(VatNumberStored(isOverseas = true, isDirectDebit = true))))

        val result = TestConfirmVatNumberController.submit(testPostRequest.withSession(SessionKeys.vatNumberKey -> testVatNumber))
        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) should contain(routes.OverseasResolverController.resolve().url)
        session(result) get SessionKeys.hasDirectDebitKey should contain("true")
      }
    }

    "vat number is in session but store vat is unsuccessful as no agent client relationship" should {
      "go to the no agent client relationship page" in {
        mockAuthRetrieveAgentEnrolment()
        mockStoreVatNumberDelegated(vatNumber = testVatNumber)(Future.successful(Left(NoAgentClientRelationship)))

        val result = TestConfirmVatNumberController.submit(testPostRequest.withSession(SessionKeys.vatNumberKey -> testVatNumber))
        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(routes.NoAgentClientRelationshipController.show().url)
      }
    }

    "vat number is in session but store vat is unsuccessful as client is ineligible" when {
      "go to the cannot use service page when no dates are given" in {
        mockAuthRetrieveAgentEnrolment()
        mockStoreVatNumberDelegated(vatNumber = testVatNumber)(Future.successful(Left(IneligibleVatNumber(MigratableDates.empty))))

        val result = TestConfirmVatNumberController.submit(testPostRequest.withSession(SessionKeys.vatNumberKey -> testVatNumber))
        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(routes.CannotUseServiceController.show().url)
      }

      "redirect to sign up after this date page when one date is available" in {
        val testDates = MigratableDates(Some(testStartDate))

        mockAuthRetrieveAgentEnrolment()
        mockStoreVatNumberDelegated(vatNumber = testVatNumber)(Future.successful(Left(IneligibleVatNumber(testDates))))

        val request = testPostRequest.withSession(SessionKeys.vatNumberKey -> testVatNumber)

        val result = TestConfirmVatNumberController.submit(request)

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(routes.MigratableDatesController.show().url)

        await(result).session(request).get(SessionKeys.migratableDatesKey) shouldBe Some(Json.toJson(testDates).toString)
      }

      "redirect to sign up between these dates page when two dates are available" in {
        val testDates = MigratableDates(Some(testStartDate), Some(testEndDate))

        mockAuthRetrieveAgentEnrolment()
        mockStoreVatNumberDelegated(vatNumber = testVatNumber)(Future.successful(Left(IneligibleVatNumber(testDates))))

        val request = testPostRequest.withSession(SessionKeys.vatNumberKey -> testVatNumber)

        val result = TestConfirmVatNumberController.submit(request)

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(routes.MigratableDatesController.show().url)

        await(result).session(request).get(SessionKeys.migratableDatesKey) shouldBe Some(Json.toJson(testDates).toString)
      }

    }

    "vat number is in session but store vat is unsuccessful" should {
      "throw internal server exception" in {
        mockAuthRetrieveAgentEnrolment()
        mockStoreVatNumberDelegated(testVatNumber)(Future.successful(Left(StoreVatNumberFailureResponse(INTERNAL_SERVER_ERROR))))

        intercept[InternalServerException] {
          await(TestConfirmVatNumberController.submit(testPostRequest.withSession(SessionKeys.vatNumberKey -> testVatNumber)))
        }
      }
    }

    "store vat is unsuccessful" when {
      "vat number is already subscribed" should {
        "redirect to the already subscribed page" in {
          mockAuthRetrieveAgentEnrolment()
          mockStoreVatNumberDelegated(testVatNumber)(Future.successful(Left(AlreadySubscribed)))

          val result = TestConfirmVatNumberController.submit(testPostRequest.withSession(SessionKeys.vatNumberKey -> testVatNumber))
          status(result) shouldBe Status.SEE_OTHER
          redirectLocation(result) shouldBe Some(routes.AlreadySignedUpController.show().url)
        }
      }

      "vat migration is in progress" should {
        "redirect to the migration in progress error page" in {
          mockAuthRetrieveAgentEnrolment()
          mockStoreVatNumberDelegated(testVatNumber)(Future.successful(Left(VatMigrationInProgress)))

          val result = TestConfirmVatNumberController.submit(testPostRequest.withSession(SessionKeys.vatNumberKey -> testVatNumber))
          status(result) shouldBe Status.SEE_OTHER
          redirectLocation(result) shouldBe Some(routes.MigrationInProgressErrorController.show().url)
        }
      }
    }

    "vat number is not in session" should {
      "redirect to capture vat number" in {
        mockAuthRetrieveAgentEnrolment()

        val result = TestConfirmVatNumberController.submit(testPostRequest)
        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(routes.CaptureVatNumberController.show().url)
      }
    }
  }

}
