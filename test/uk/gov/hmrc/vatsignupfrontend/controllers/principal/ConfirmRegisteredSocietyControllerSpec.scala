/*
 * Copyright 2018 HM Revenue & Customs
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

package uk.gov.hmrc.vatsignupfrontend.controllers.principal

import org.jsoup.Jsoup
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.Status
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.http.InternalServerException
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.vatsignupfrontend.SessionKeys
import uk.gov.hmrc.vatsignupfrontend.config.mocks.MockControllerComponents
import uk.gov.hmrc.vatsignupfrontend.helpers.TestConstants._
import uk.gov.hmrc.vatsignupfrontend.services.mocks.MockStoreRegisteredSocietyService
import uk.gov.hmrc.vatsignupfrontend.config.featureswitch.{FeatureSwitching, RegisteredSocietyJourney}

class ConfirmRegisteredSocietyControllerSpec extends UnitSpec with GuiceOneAppPerSuite with MockControllerComponents
  with MockStoreRegisteredSocietyService with FeatureSwitching {

  override def beforeEach(): Unit = {
    super.beforeEach()
    enable(RegisteredSocietyJourney)
  }

  override def afterEach(): Unit = {
    super.afterEach()
    enable(RegisteredSocietyJourney)
  }

  object TestConfirmRegisteredSocietyController extends ConfirmRegisteredSocietyController(
    mockControllerComponents,
    mockStoreRegisteredSocietyService
  )

  val testGetRequest = FakeRequest("GET", "/confirm-registered-society")

  val testPostRequest: FakeRequest[AnyContentAsEmpty.type] =
    FakeRequest("POST", "/confirm-registered-society")

  "Calling the show action of the Confirm Registered Society controller" when {
    "there is a registered society name in the session" should {
      "go to the Confirm Registered Society page" in {
        mockAuthAdminRole()
        val request = testGetRequest.withSession(
          SessionKeys.registeredSocietyNameKey -> testCompanyName
        )

        val result = TestConfirmRegisteredSocietyController.show(request)
        status(result) shouldBe Status.OK
        contentType(result) shouldBe Some("text/html")
        charset(result) shouldBe Some("utf-8")

        val changeLink = Jsoup.parse(contentAsString(result)).getElementById("changeLink")
        changeLink.attr("href") shouldBe routes.CaptureRegisteredSocietyCompanyNumberController.show().url
      }
    }

    "there isn't a registered society name in the session" should {
      "go to the capture registered society company number page" in {
        mockAuthAdminRole()

        val result = TestConfirmRegisteredSocietyController.show(testGetRequest)
        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(routes.CaptureRegisteredSocietyCompanyNumberController.show().url)
      }
    }
  }

  "Calling the submit action of the Confirm Registered Society controller" should {
    "go to the 'agree to receive emails' page" in {
      mockAuthAdminRole()
      mockStoreRegisteredSocietySuccess(
        vatNumber = testVatNumber,
        companyNumber = testCompanyNumber
      )

      val request = testPostRequest.withSession(
        SessionKeys.vatNumberKey -> testVatNumber,
        SessionKeys.registeredSocietyCompanyNumberKey -> testCompanyNumber
      )

      val result = TestConfirmRegisteredSocietyController.submit(request)
      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result) shouldBe Some(routes.AgreeCaptureEmailController.show().url)
    }

    "throw internal server exception if store registered society fails" in {
      mockAuthAdminRole()
      mockStoreRegisteredSocietyFailure(testVatNumber, testCompanyNumber)

      val request = testPostRequest.withSession(
        SessionKeys.vatNumberKey -> testVatNumber,
        SessionKeys.registeredSocietyCompanyNumberKey -> testCompanyNumber
      )

      intercept[InternalServerException] {
        await(TestConfirmRegisteredSocietyController.submit(request))
      }
    }
    "go to the 'your vat number' page if vat number is missing" in {
      mockAuthAdminRole()

      val request = testPostRequest.withSession(
        SessionKeys.registeredSocietyCompanyNumberKey -> testCompanyNumber
      )

      val result = TestConfirmRegisteredSocietyController.submit(request)
      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result) shouldBe Some(routes.ResolveVatNumberController.resolve().url)

    }
    "go to the 'capture registered society company number' page if company number is missing" in {
      mockAuthAdminRole()

      val request = testPostRequest.withSession(
        SessionKeys.vatNumberKey -> testVatNumber
      )

      val result = TestConfirmRegisteredSocietyController.submit(request)
      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result) shouldBe Some(routes.CaptureRegisteredSocietyCompanyNumberController.show().url)
    }
  }

}