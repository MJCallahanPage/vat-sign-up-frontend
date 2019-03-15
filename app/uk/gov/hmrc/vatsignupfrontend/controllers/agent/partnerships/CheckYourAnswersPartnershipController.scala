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

package uk.gov.hmrc.vatsignupfrontend.controllers.agent.partnerships

import javax.inject.{Inject, Singleton}
import play.api.mvc.{Action, AnyContent, Result}
import uk.gov.hmrc.http.{InternalServerException, NotFoundException}
import uk.gov.hmrc.vatsignupfrontend.SessionKeys
import uk.gov.hmrc.vatsignupfrontend.config.ControllerComponents
import uk.gov.hmrc.vatsignupfrontend.config.auth.AgentEnrolmentPredicate
import uk.gov.hmrc.vatsignupfrontend.config.featureswitch.{GeneralPartnershipJourney, LimitedPartnershipJourney}
import uk.gov.hmrc.vatsignupfrontend.controllers.AuthenticatedController
import uk.gov.hmrc.vatsignupfrontend.controllers.agent.{routes => agentRoutes}
import uk.gov.hmrc.vatsignupfrontend.httpparsers.StoreJointVentureInformationHttpParser.{StoreJointVentureInformationFailureResponse, StoreJointVentureInformationSuccess}
import uk.gov.hmrc.vatsignupfrontend.httpparsers.StorePartnershipInformationHttpParser._
import uk.gov.hmrc.vatsignupfrontend.models._
import uk.gov.hmrc.vatsignupfrontend.services.{StoreJointVentureInformationService, StorePartnershipInformationService}
import uk.gov.hmrc.vatsignupfrontend.utils.SessionUtils._
import uk.gov.hmrc.vatsignupfrontend.views.html.agent.partnerships.check_your_answers

import scala.concurrent.Future

@Singleton
class CheckYourAnswersPartnershipController @Inject()(val controllerComponents: ControllerComponents,
                                                      val storePartnershipInformationService: StorePartnershipInformationService,
                                                      val storeJointVentureInformationService: StoreJointVentureInformationService)
  extends AuthenticatedController(AgentEnrolmentPredicate, featureSwitches = Set(GeneralPartnershipJourney, LimitedPartnershipJourney)) {

  override protected def featureEnabled[T](func: => T): T =
    if (featureSwitches exists isEnabled) func
    else throw new NotFoundException(featureSwitchError)

  def show: Action[AnyContent] = Action.async { implicit request =>
    authorised() {

      val optVatNumber = request.session.get(SessionKeys.vatNumberKey).filter(_.nonEmpty)
      val optBusinessEntityType = request.session.getModel[BusinessEntity](SessionKeys.businessEntityKey)
      val optJointVentureProperty = request.session.get(SessionKeys.jointVentureOrPropertyKey).map(_.toBoolean)
      val optPartnershipUtr = request.session.get(SessionKeys.partnershipSautrKey).filter(_.nonEmpty)
      val optPartnershipPostCode = request.session.getModel[PostCode](SessionKeys.partnershipPostCodeKey)
      val optPartnershipCrn = request.session.get(SessionKeys.companyNumberKey).filter(_.nonEmpty)

      (optVatNumber, optJointVentureProperty, optBusinessEntityType, optPartnershipCrn, optPartnershipUtr, optPartnershipPostCode) match {
        case (Some(_), Some(true), Some(GeneralPartnership), _, _, _) =>
          Future.successful(Ok(check_your_answers(
            entityType = GeneralPartnership,
            utr = None,
            companyNumber = None,
            postCode = None,
            jointVentureProperty = optJointVentureProperty,
            postAction = routes.CheckYourAnswersPartnershipController.submit()
          )))
        case (Some(_), _, Some(GeneralPartnership), _, Some(utr), Some(postcode)) =>
          Future.successful(Ok(check_your_answers(
            entityType = GeneralPartnership,
            utr = Some(utr),
            companyNumber = None,
            postCode = Some(postcode),
            jointVentureProperty = optJointVentureProperty,
            postAction = routes.CheckYourAnswersPartnershipController.submit()
          )))
        case (Some(_), _, Some(entity: LimitedPartnershipBase), Some(crn), Some(utr), Some(postcode)) =>
          Future.successful(Ok(check_your_answers(
            entityType = entity,
            utr = Some(utr),
            companyNumber = Some(crn),
            postCode = Some(postcode),
            jointVentureProperty = None,
            postAction = routes.CheckYourAnswersPartnershipController.submit()
          )))
        case (None, _, _, _, _, _) =>
          Future.successful(Redirect(agentRoutes.CaptureVatNumberController.show()))
        case _ =>
          Future.successful(Redirect(agentRoutes.CaptureBusinessEntityController.show()))
      }
    }
  }


  def submit: Action[AnyContent] = Action.async { implicit request =>
    authorised() {

      val optVatNumber = request.session.get(SessionKeys.vatNumberKey).filter(_.nonEmpty)
      val optBusinessEntityType = request.session.getModel[BusinessEntity](SessionKeys.businessEntityKey)
      val optJointVentureProperty = request.session.getModel[Boolean](SessionKeys.jointVentureOrPropertyKey)
      val optPartnershipUtr = request.session.get(SessionKeys.partnershipSautrKey).filter(_.nonEmpty)
      val optPartnershipPostCode = request.session.getModel[PostCode](SessionKeys.partnershipPostCodeKey)
      val optPartnershipCrn = request.session.get(SessionKeys.companyNumberKey).filter(_.nonEmpty)
      val optPartnershipType = request.session.getModel[PartnershipEntityType](SessionKeys.partnershipTypeKey)

      (optVatNumber, optJointVentureProperty, optBusinessEntityType) match {
        case (Some(vrn), Some(true), Some(GeneralPartnership)) =>
          storeJointVentureInformationService.storeJointVentureInformation(vrn) map {
            case Right(StoreJointVentureInformationSuccess) => Redirect(agentRoutes.EmailRoutingController.route())
            case Left(StoreJointVentureInformationFailureResponse(status)) =>
              throw new InternalServerException("Store Joint Venture Partnership Information failed with status code: " + status)
          }
        case (Some(vrn), _, Some(GeneralPartnership)) =>
          (optPartnershipUtr, optPartnershipPostCode) match {
            case (Some(utr), Some(postcode)) =>
              storePartnershipInformationService.storePartnershipInformation(
                vatNumber = vrn,
                sautr = utr,
                postCode = Some(postcode)
              ) map handleStorePartnershipResult
            case _ =>
              Future.successful(Redirect(agentRoutes.CaptureBusinessEntityController.show()))
          }
        case (Some(vrn), _, Some(entity: LimitedPartnershipBase)) =>
          (optPartnershipUtr, optPartnershipPostCode, optPartnershipCrn, optPartnershipType) match {
            case (Some(utr), Some(postcode), Some(crn), Some(partnershipType)) =>
              storePartnershipInformationService.storePartnershipInformation(
                vatNumber = vrn,
                sautr = utr,
                companyNumber = crn,
                partnershipEntity = partnershipType,
                postCode = Some(postcode)
              ) map handleStorePartnershipResult
            case _ =>
              Future.successful(Redirect(agentRoutes.CaptureBusinessEntityController.show()))
          }
        case (None, _, _) =>
          Future.successful(Redirect(agentRoutes.CaptureVatNumberController.show()))
        case _ =>
          Future.successful(Redirect(agentRoutes.CaptureBusinessEntityController.show()))
      }
    }
  }

  private val handleStorePartnershipResult: StorePartnershipInformationResponse => Result = {
    case Right(StorePartnershipInformationSuccess) =>
      Redirect(agentRoutes.EmailRoutingController.route())
    case Left(PartnershipUtrNotFound) =>
      Redirect(routes.CouldNotConfirmPartnershipController.show())
    case Left(StorePartnershipKnownFactsFailure) =>
      Redirect(routes.CouldNotConfirmPartnershipController.show())
    case Left(StorePartnershipInformationFailureResponse(status)) =>
      throw new InternalServerException("Store Partnership failed with status code: " + status)
  }
}
