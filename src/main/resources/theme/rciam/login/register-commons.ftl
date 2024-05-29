<#macro termsAcceptance>
    <#if termsAcceptanceRequired?? && termsAcceptanceRequired>
        <div class="form-group">
            <div class="${properties.kcLabelWrapperClass!}">
                <input type="checkbox" id="termsAccepted" name="termsAccepted" class="${properties.kcCheckboxInputClass!}"
                       aria-invalid="<#if messagesPerField.existsError('termsAccepted')>true</#if>"
                />
                <label for="termsAccepted" class="${properties.kcLabelClass!}">${msg("acceptTermsTxt")} <a href="${uriInfo.baseUri}realms/${realm.name}/theme-info/terms-of-use"}" target="_blank" >${kcSanitize(msg("acceptTerms"))?no_esc}</a></label>
            </div>
            <#if messagesPerField.existsError('termsAccepted')>
                <div class="${properties.kcLabelWrapperClass!}">
                            <span id="input-error-terms-accepted" class="${properties.kcInputErrorMessageClass!}" aria-live="polite">
                                ${kcSanitize(messagesPerField.get('termsAccepted'))?no_esc}
                            </span>
                </div>
            </#if>
        </div>
    </#if>
</#macro>
