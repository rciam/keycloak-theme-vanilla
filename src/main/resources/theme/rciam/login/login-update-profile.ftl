<#import "template.ftl" as layout>
<#import "register-commons.ftl" as registerCommons>
<@layout.registrationLayout displayMessage=!messagesPerField.existsError('username','email','firstName','lastName','termsAccepted'); section>
    <#if section = "header">
        <b>${msg("loginProfileTitle")}</b>
    <#elseif section = "form">
        <form id="kc-update-profile-form" class="${properties.kcFormClass!}" action="${url.loginAction}" method="post">
            <div class="form-group">
                <div class="${properties.kcInputWrapperClass!}">
                    ${msg("loginProfileDescription")}
                </div>
            </div>
            <#if user.editUsernameAllowed>
              <div class="${properties.kcFormGroupClass!} <#if hideUsername?? && hideUsername>dnone</#if>">
                <div class="${properties.kcLabelWrapperClass!}">
                  <label for="username" class="${properties.kcLabelClass!}">${msg("username")}</label>
                </div>
                <div class="${properties.kcInputWrapperClass!}">
                  <input type="text" id="username" name="username" value="${(user.username!'')}"
                    class="${properties.kcInputClass!}" <#if (updateProfileFirstLogin?has_content && updateProfileFirstLogin == 'off') || ( updateProfileFirstLogin?has_content && updateProfileFirstLogin == 'missing-only' && user.username?has_content)>disabled</#if>
                    aria-invalid="<#if messagesPerField.existsError('username')>true</#if>"
                />

                  <#if messagesPerField.existsError('username')>
                    <span id="input-error-username" class="${properties.kcInputErrorMessageClass!}" aria-live="polite">
                      ${kcSanitize(messagesPerField.get('username'))?no_esc}
                    </span>
                  </#if>
                </div>
              </div>
            </#if>
            <#if user.editEmailAllowed>
                <div class="${properties.kcFormGroupClass!}">
                    <div class="${properties.kcLabelWrapperClass!}">
                        <label for="email" class="${properties.kcLabelClass!}">${msg("email")}</label>
                    </div>
                    <div class="${properties.kcInputWrapperClass!}">
                        <input type="text" id="email" name="email" value="${(user.email!'')}"
                               class="${properties.kcInputClass!}" <#if (updateProfileFirstLogin?has_content && updateProfileFirstLogin == 'off') || ( updateProfileFirstLogin?has_content && updateProfileFirstLogin == 'missing-only' && user.email?has_content) >disabled</#if>
                               aria-invalid="<#if messagesPerField.existsError('email')>true</#if>"
                        />

                        <#if messagesPerField.existsError('email')>
                            <span id="input-error-email" class="${properties.kcInputErrorMessageClass!}" aria-live="polite">
                                ${kcSanitize(messagesPerField.get('email'))?no_esc}
                            </span>
                        </#if>
                    </div>
                </div>
            </#if>

            <div class="${properties.kcFormGroupClass!}">
                <div class="${properties.kcLabelWrapperClass!}">
                    <label for="firstName" class="${properties.kcLabelClass!}">${msg("firstName")}</label>
                </div>
                <div class="${properties.kcInputWrapperClass!}">
                    <input type="text" id="firstName" name="firstName" value="${(user.firstName!'')}"
                           class="${properties.kcInputClass!}" <#if (updateProfileFirstLogin?has_content && updateProfileFirstLogin == 'off') || ( updateProfileFirstLogin?has_content && updateProfileFirstLogin == 'missing-only' && user.firstName?has_content)>disabled</#if>
                           aria-invalid="<#if messagesPerField.existsError('firstName')>true</#if>"
                    />

                    <#if messagesPerField.existsError('firstName')>
                        <span id="input-error-firstname" class="${properties.kcInputErrorMessageClass!}" aria-live="polite">
                            ${kcSanitize(messagesPerField.get('firstName'))?no_esc}
                        </span>
                    </#if>
                </div>
            </div>

            <div class="${properties.kcFormGroupClass!}">
                <div class="${properties.kcLabelWrapperClass!}">
                    <label for="lastName" class="${properties.kcLabelClass!}">${msg("lastName")}</label>
                </div>
                <div class="${properties.kcInputWrapperClass!}">
                    <input type="text" id="lastName" name="lastName" value="${(user.lastName!'')}"
                           class="${properties.kcInputClass!}" <#if (updateProfileFirstLogin?has_content && updateProfileFirstLogin == 'off') || ( updateProfileFirstLogin?has_content && updateProfileFirstLogin == 'missing-only' && user.lastName?has_content)>readonly</#if>
                           aria-invalid="<#if messagesPerField.existsError('lastName')>true</#if>"
                    />

                    <#if messagesPerField.existsError('lastName')>
                        <span id="input-error-lastname" class="${properties.kcInputErrorMessageClass!}" aria-live="polite">
                            ${kcSanitize(messagesPerField.get('lastName'))?no_esc}
                        </span>
                    </#if>
                </div>
            </div>
            <#if createUid?? && createUid>
                <div class="${properties.kcFormGroupClass!}">
                    <div class="${properties.kcLabelWrapperClass!}">
                        <label for="uid" class="${properties.kcLabelClass!}">
                            ${msg("uid")}
                            <span class="kc-tooltip-icon pf-info-tooltip" title="${msg("uidTooltip")}">
                                <i class="pf-icon pf-icon-info"></i>
                            </span>
                        </label>
                    </div>
                    <div class="${properties.kcInputWrapperClass!}">
                        <input type="text" id="user.attributes.uid" name="user.attributes.uid" value="${(user.attributes.uid!'')}"
                            class="${properties.kcInputClass!}"
                            aria-invalid="<#if messagesPerField.existsError('user.attributes.uid')>true</#if>"
                        />
                        <#if messagesPerField.existsError('uid')>
                            <span id="input-error-uid" class="${properties.kcInputErrorMessageClass!}" aria-live="polite">
                                ${kcSanitize(messagesPerField.get('user.attributes.uid'))?no_esc}
                            </span>
                        </#if>
                    </div>
                </div>
            </#if>
            <@registerCommons.termsAcceptance/>
            <div class="${properties.kcFormGroupClass!}">
                <div id="kc-form-options" class="${properties.kcFormOptionsClass!}">
                    <div class="${properties.kcFormOptionsWrapperClass!}">
                    </div>
                </div>

                <div id="kc-form-buttons" class="${properties.kcFormButtonsClass!}">
                    <#if isAppInitiatedAction??>
                    <input class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonLargeClass!}" type="submit" value="${msg("doSignUp")}" />
                    <button class="${properties.kcButtonClass!} ${properties.kcButtonDefaultClass!} ${properties.kcButtonLargeClass!}" type="submit" name="cancel-aia" value="true" />${msg("doCancel")}</button>
                    <#else>
                    <input class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!} ${properties.kcButtonLargeClass!}" type="submit" value="${msg("doSignUp")}" />
                    </#if>
                </div>
            </div>
        </form>
    </#if>
</@layout.registrationLayout>
