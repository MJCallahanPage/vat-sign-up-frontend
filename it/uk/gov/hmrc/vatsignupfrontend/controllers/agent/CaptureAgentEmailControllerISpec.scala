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

package uk.gov.hmrc.vatsignupfrontend.controllers.agent

import play.api.http.Status._
import uk.gov.hmrc.vatsignupfrontend.SessionKeys._
import uk.gov.hmrc.vatsignupfrontend.config.featureswitch.VerifyAgentEmail
import uk.gov.hmrc.vatsignupfrontend.forms.EmailForm
import uk.gov.hmrc.vatsignupfrontend.helpers.IntegrationTestConstants._
import uk.gov.hmrc.vatsignupfrontend.helpers.servicemocks.AuthStub._
import uk.gov.hmrc.vatsignupfrontend.helpers.{ComponentSpecBase, CustomMatchers, SessionCookieCrumbler}

class CaptureAgentEmailControllerISpec extends ComponentSpecBase with CustomMatchers {

  override def beforeEach(): Unit = {
    super.beforeEach()
    enable(VerifyAgentEmail)
  }

  override def afterEach(): Unit = {
    super.afterEach()
    disable(VerifyAgentEmail)
  }

  "GET /your-email-address" should {
    "return an OK" in {
      stubAuth(OK, successfulAuthResponse(agentEnrolment))

      val res = get("/client/your-email-address")

      res should have(
        httpStatus(OK)
      )
    }
    "return an NOT_FOUND if VerifyAgentEmail is disabled" in {
      disable(VerifyAgentEmail)
      stubAuth(OK, successfulAuthResponse(agentEnrolment))

      val res = get("/client/your-email-address")

      res should have(
        httpStatus(NOT_FOUND)
      )
    }
  }

  "POST /your-email-address" should {
    "return a redirect" in {
      stubAuth(OK, successfulAuthResponse(agentEnrolment))

      val res = post("/client/your-email-address")(EmailForm.email -> testEmail)

      res should have(
        httpStatus(SEE_OTHER),
        redirectUri(routes.ConfirmAgentEmailController.show().url)
      )

      val session = SessionCookieCrumbler.getSessionMap(res)
      session.keys should contain(emailKey)
    }
    "return an NOT_FOUND if VerifyAgentEmail is disabled" in {
      disable(VerifyAgentEmail)
      stubAuth(OK, successfulAuthResponse(agentEnrolment))

      val res = post("/client/your-email-address")(EmailForm.email -> testEmail)

      res should have(
        httpStatus(NOT_FOUND)
      )
    }
  }

}
