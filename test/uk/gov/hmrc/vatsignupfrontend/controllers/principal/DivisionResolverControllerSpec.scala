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

package uk.gov.hmrc.vatsignupfrontend.controllers.principal

import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.http.{InternalServerException, NotFoundException}
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.vatsignupfrontend.SessionKeys
import uk.gov.hmrc.vatsignupfrontend.config.featureswitch.DivisionJourney
import uk.gov.hmrc.vatsignupfrontend.config.mocks.MockControllerComponents
import uk.gov.hmrc.vatsignupfrontend.helpers.TestConstants._
import uk.gov.hmrc.vatsignupfrontend.httpparsers.StoreAdministrativeDivisionHttpParser.{StoreAdministrativeDivisionFailureResponse, StoreAdministrativeDivisionSuccess}
import uk.gov.hmrc.vatsignupfrontend.services.mocks.MockStoreAdministrativeDivisionService

import scala.concurrent.Future

class DivisionResolverControllerSpec extends UnitSpec with GuiceOneAppPerSuite with MockControllerComponents
  with MockStoreAdministrativeDivisionService {

  override def beforeEach(): Unit = {
    super.beforeEach()
    enable(DivisionJourney)
  }

  object TestDivisionResolverController extends DivisionResolverController(
    mockControllerComponents,
    mockStoreAdministrativeDivisionService
  )

  lazy val testGetRequest = FakeRequest("GET", "/division-resolver")

  "calling the resolve method on DivisionResolverController" when {
    "the group feature switch is on" when {
      "store group information returns StoreDivisionInformationSuccess" should {
        "goto agree capture email" in {
          mockAuthAdminRole()
          mockStoreAdministrativeDivision(testVatNumber)(Future.successful(Right(StoreAdministrativeDivisionSuccess)))

          val res = await(TestDivisionResolverController.resolve(testGetRequest.withSession(
            SessionKeys.vatNumberKey -> testVatNumber
          )))

          status(res) shouldBe SEE_OTHER
          redirectLocation(res) shouldBe Some(routes.DirectDebitResolverController.show().url)
        }
      }
      "store group information returns StoreDivisionInformationFailureResponse" should {
        "throw internal server exception" in {
          mockAuthAdminRole()
          mockStoreAdministrativeDivision(testVatNumber)(Future.successful(Left(StoreAdministrativeDivisionFailureResponse(INTERNAL_SERVER_ERROR))))

          intercept[InternalServerException] {
            await(TestDivisionResolverController.resolve(testGetRequest.withSession(
              SessionKeys.vatNumberKey -> testVatNumber
            )))
          }
        }
      }
      "vat number is not in session" should {
        "goto resolve vat number" in {
          mockAuthAdminRole()
          mockStoreAdministrativeDivision(testVatNumber)(Future.successful(Right(StoreAdministrativeDivisionSuccess)))

          val res = await(TestDivisionResolverController.resolve(testGetRequest))

          status(res) shouldBe SEE_OTHER
          redirectLocation(res) shouldBe Some(routes.ResolveVatNumberController.resolve().url)
        }
      }
    }

    "the group feature switch is off" should {
      "throw not found exception" in {
        disable(DivisionJourney)

        intercept[NotFoundException] {
          await(TestDivisionResolverController.resolve(testGetRequest.withSession(
            SessionKeys.vatNumberKey -> testVatNumber
          )))
        }
      }
    }
  }

}

