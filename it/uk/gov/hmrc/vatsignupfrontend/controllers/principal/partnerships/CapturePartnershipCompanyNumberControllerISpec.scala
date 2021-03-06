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
import uk.gov.hmrc.vatsignupfrontend.config.featureswitch.LimitedPartnershipJourney
import uk.gov.hmrc.vatsignupfrontend.forms.CompanyNumberForm
import uk.gov.hmrc.vatsignupfrontend.helpers.IntegrationTestConstants._
import uk.gov.hmrc.vatsignupfrontend.helpers.servicemocks.AuthStub._
import uk.gov.hmrc.vatsignupfrontend.helpers.servicemocks.GetCompanyNameStub.stubGetCompanyName
import uk.gov.hmrc.vatsignupfrontend.helpers.{ComponentSpecBase, CustomMatchers}
import uk.gov.hmrc.vatsignupfrontend.models.companieshouse.{LimitedPartnership, NonPartnershipEntity}

class CapturePartnershipCompanyNumberControllerISpec extends ComponentSpecBase with CustomMatchers {

  override def beforeEach(): Unit = {
    super.beforeEach()
    enable(LimitedPartnershipJourney)
  }

  override def afterEach(): Unit = {
    super.afterEach()
    enable(LimitedPartnershipJourney)
  }

  "GET /partnership-company-number" should {
    "return an OK" in {
      stubAuth(OK, successfulAuthResponse())

      val res = get("/partnership-company-number")

      res should have(
        httpStatus(OK)
      )
    }

    "return an NOT FOUND" in {
      disable(LimitedPartnershipJourney)
      stubAuth(OK, successfulAuthResponse())

      val res = get("/partnership-company-number")

      res should have(
        httpStatus(NOT_FOUND)
      )
    }
  }

  "POST /partnership-company-number" when {
    "companies house returned limited partnership" should {
      "redirect to Confirm Partnership page" in {
        stubAuth(OK, successfulAuthResponse())
        stubGetCompanyName(testCompanyNumber, LimitedPartnership)

        val res = post("/partnership-company-number")(CompanyNumberForm.companyNumber -> testCompanyNumber)

        res should have(
          httpStatus(SEE_OTHER),
          redirectUri(routes.ConfirmPartnershipController.show().url)
        )
      }
    }
    "companies house returned none partnership entity" should {
      "redirect to Could not Confirm limited Partnership page" in {
        stubAuth(OK, successfulAuthResponse())
        stubGetCompanyName(testCompanyNumber, NonPartnershipEntity)

        val res = post("/partnership-company-number")(CompanyNumberForm.companyNumber -> testCompanyNumber)

        res should have(
          httpStatus(SEE_OTHER),
          redirectUri(routes.CouldNotConfirmLimitedPartnershipController.show().url)
        )
      }
    }
  }

}
