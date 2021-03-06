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
import uk.gov.hmrc.vatsignupfrontend.SessionKeys
import uk.gov.hmrc.vatsignupfrontend.config.featureswitch.ContactPreferencesJourney
import uk.gov.hmrc.vatsignupfrontend.forms.ContactPreferencesForm
import uk.gov.hmrc.vatsignupfrontend.helpers.IntegrationTestConstants._
import uk.gov.hmrc.vatsignupfrontend.helpers.servicemocks.AuthStub._
import uk.gov.hmrc.vatsignupfrontend.helpers.servicemocks.StoreContactPreferenceStub._
import uk.gov.hmrc.vatsignupfrontend.helpers.{ComponentSpecBase, CustomMatchers}
import uk.gov.hmrc.vatsignupfrontend.models.Digital

class ContactPreferenceControllerISpec extends ComponentSpecBase with CustomMatchers {

  override def beforeEach(): Unit = {
    super.beforeEach()
    enable(ContactPreferencesJourney)
  }

  "GET /receive-email-notifications" should {

    "return an OK" in {
      stubAuth(OK, successfulAuthResponse(agentEnrolment))

      val res = get("/client/receive-email-notifications")

      res should have(
        httpStatus(OK)
      )
    }
  }

  "POST /receive-email-notifications" when {

    "Storing the contact-preference is successful" should {

      "return a redirect" in {
        stubAuth(OK, successfulAuthResponse(agentEnrolment))
        stubStoreContactPreferenceSuccess(Digital)

        val res = post(
          "/client/receive-email-notifications",
          Map(SessionKeys.vatNumberKey -> testVatNumber)
        )(ContactPreferencesForm.contactPreference -> ContactPreferencesForm.digital)

        res should have(
          httpStatus(SEE_OTHER),
          redirectUri(routes.CaptureClientEmailController.show().url)
        )
      }
    }

    "Storing the contact-preference is NOT successful" should {

      "return a redirect" in {
        stubAuth(OK, successfulAuthResponse(agentEnrolment))
        stubStoreContactPreferenceFailure(Digital)

        val res = post(
          "/client/receive-email-notifications",
          Map(SessionKeys.vatNumberKey -> testVatNumber)
        )(ContactPreferencesForm.contactPreference -> ContactPreferencesForm.digital)

        res should have(
          httpStatus(INTERNAL_SERVER_ERROR)
        )
      }
    }
  }
}
