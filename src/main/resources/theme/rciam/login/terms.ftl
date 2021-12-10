<#import "template.ftl" as layout>

<#import "commons.ftl" as commons>

<@commons.variables />
<@commons.functions />

<script>

    fetch(baseUri + 'realms/' + realm + '/theme-info/terms-of-use?complete=false').then(response => response.text()).then(termsOfUseHtmlText => {
        Array.from(document.getElementsByClassName("card-pf")).forEach(elem => { elem.style.maxWidth = 1000 })
        Array.from(document.getElementsByClassName("login-pf-header")).forEach(elem => elem.remove());
        Array.from(document.getElementsByClassName("login-pf-page")).forEach(elem => { elem.style.paddingBottom = 50 });
        document.getElementById("kc-terms-text").innerHTML = termsOfUseHtmlText;
    });

    drawFooterInPlace();

    document.addEventListener("DOMContentLoaded", function(event) {
        removeDefaultLogo();
    });


</script>



<@layout.registrationLayout displayMessage=false; section>

    <#if section = "form">
        <div id="kc-terms-text">

        </div>
        <form class="form-actions" action="${url.loginAction}" method="POST">
            <input class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonLargeClass!}" name="accept" id="kc-accept" type="submit" value="${msg("doAccept")}"/>
            <input class="${properties.kcButtonClass!} ${properties.kcButtonDefaultClass!} ${properties.kcButtonLargeClass!}" name="cancel" id="kc-decline" type="submit" value="${msg("doDecline")}"/>
        </form>
        <div class="clearfix"></div>
    </#if>
</@layout.registrationLayout>
