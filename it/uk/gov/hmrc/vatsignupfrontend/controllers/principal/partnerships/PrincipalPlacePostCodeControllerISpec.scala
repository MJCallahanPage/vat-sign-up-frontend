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

package uk.gov.hmrc.vatsignupfrontend.controllers.principal.partnerships

import play.api.http.Status._
import uk.gov.hmrc.vatsignupfrontend.config.featureswitch.GeneralPartnershipJourney
import uk.gov.hmrc.vatsignupfrontend.forms.PartnershipPostCodeForm
import uk.gov.hmrc.vatsignupfrontend.helpers.IntegrationTestConstants._
import uk.gov.hmrc.vatsignupfrontend.helpers.servicemocks.AuthStub._
import uk.gov.hmrc.vatsignupfrontend.helpers.{ComponentSpecBase, CustomMatchers}

class PrincipalPlacePostCodeControllerISpec extends ComponentSpecBase with CustomMatchers {

  override def beforeEach(): Unit = {
    super.beforeEach()
    enable(GeneralPartnershipJourney)
  }

  override def afterEach(): Unit = {
    super.afterEach()
    disable(GeneralPartnershipJourney)
  }

  "GET /principal-place-postcode" should {

    "return an OK" in {
      stubAuth(OK, successfulAuthResponse())

      val res = get("/principal-place-postcode")

      res should have(
        httpStatus(OK)
      )
    }

    "return an Not Found if the feature switch is disabled" in {
      disable(GeneralPartnershipJourney)
      stubAuth(OK, successfulAuthResponse())

      val res = get("/principal-place-postcode")

      res should have(
        httpStatus(NOT_FOUND)
      )
    }

  }


  "POST /principal-place-postcode" should {

    "redirect to partnership CYA page" in {
      stubAuth(OK, successfulAuthResponse())

      val res = post("/principal-place-postcode")(PartnershipPostCodeForm.partnershipPostCode -> testBusinessPostCode.postCode)

      res should have(
        httpStatus(SEE_OTHER),
          redirectUri(routes.CheckYourAnswersPartnershipsController.show().url)
      )
    }

    "return an Not Found if the feature switch is disabled" in {
      disable(GeneralPartnershipJourney)
      stubAuth(OK, successfulAuthResponse())

      val res = post("/principal-place-postcode")(PartnershipPostCodeForm.partnershipPostCode -> testBusinessPostCode.postCode)

      res should have(
        httpStatus(NOT_FOUND)
      )
    }

  }

}