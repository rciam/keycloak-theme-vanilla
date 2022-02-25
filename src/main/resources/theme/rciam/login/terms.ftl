<#import "template.ftl" as layout>

<script>

    document.addEventListener('DOMContentLoaded', function(event) {
        Array.from(document.getElementsByClassName("card-pf")).forEach(elem => { elem.style.maxWidth = 1000 })
        Array.from(document.getElementsByClassName("login-pf-header")).forEach(elem => elem.remove());
        Array.from(document.getElementsByClassName("login-pf-page")).forEach(elem => { elem.style.paddingBottom = 50 });
        document.getElementById("kc-terms-text").innerHTML = "<iframe id='tou-frame' frameborder='0' style='position: relative;  width: 100%;' src=" + baseUri + '/realms/' + realm + '/theme-info/terms-of-use?complete=true' +" title='Terms of use'>";
        var iframe = document.getElementById("tou-frame");
        iframe.onload = function(){
            iframe.style.height = (iframe.contentWindow.document.body.scrollHeight + 70) + 'px';
        }
    })


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
