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

package uk.gov.hmrc.vatsignupfrontend.controllers.principal.partnerships

import javax.inject.{Inject, Singleton}
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.auth.core.retrieve.Retrievals
import uk.gov.hmrc.http.InternalServerException
import uk.gov.hmrc.vatsignupfrontend.SessionKeys
import uk.gov.hmrc.vatsignupfrontend.config.ControllerComponents
import uk.gov.hmrc.vatsignupfrontend.config.auth.AdministratorRolePredicate
import uk.gov.hmrc.vatsignupfrontend.config.featureswitch.{GeneralPartnershipJourney, LimitedPartnershipJourney}
import uk.gov.hmrc.vatsignupfrontend.controllers.AuthenticatedController
import uk.gov.hmrc.vatsignupfrontend.utils.EnrolmentUtils._

import scala.concurrent.Future

@Singleton
class ResolvePartnershipUtrController @Inject()(val controllerComponents: ControllerComponents)
  extends AuthenticatedController(AdministratorRolePredicate) {

  val resolve: Action[AnyContent] = Action.async { implicit request =>
    authorised()(Retrievals.allEnrolments) { enrolments =>

      val optCompanyName = request.session.get(SessionKeys.companyNameKey).filter(_.nonEmpty)
      val optCompanyNumber = request.session.get(SessionKeys.companyNumberKey).filter(_.nonEmpty)
      val optPartnershipType = request.session.get(SessionKeys.partnershipTypeKey).filter(_.nonEmpty)

      if (isEnabled(GeneralPartnershipJourney) || isEnabled(LimitedPartnershipJourney)) {
        (enrolments.partnershipUtr, optCompanyName, optCompanyNumber, optPartnershipType) match {
          case (Some(partnershipUtr), Some(_), Some(_), Some(_)) =>
            Future.successful(
              Redirect(routes.ConfirmLimitedPartnershipController.show())
                addingToSession SessionKeys.partnershipSautrKey -> partnershipUtr
            )
          case (Some(partnershipUtr), None, None, None) =>
            Future.successful(
              Redirect(routes.ConfirmGeneralPartnershipController.show())
                addingToSession SessionKeys.partnershipSautrKey -> partnershipUtr
            )
          case (Some(_), _, _, _) =>
            Future.successful(Redirect(routes.CapturePartnershipCompanyNumberController.show()))
          case _ =>
            Future.successful(Redirect(routes.CapturePartnershipUtrController.show()))
        }
      } else
        Future.failed(new InternalServerException("Both feature switches are disabled"))
    }
  }

}
