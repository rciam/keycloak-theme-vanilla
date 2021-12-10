<#macro variables>

    <script>
        var realm = '${realm.name}';
        var baseUri = '${uriInfo.baseUri}';
        var resourcesCommonPath = '${url.resourcesCommonPath}';
        var resourcesPath = '${url.resourcesPath}';
    </script>

</#macro>

<#macro functions>

    <script>

        function getConfig() {
            fetch(baseUri + 'realms/' + realm + '/theme-info/theme-config').then(response => response.json()).then(config => {
                applyConfig(config);
            });
        }


        function applyConfig(config){

            //set main logo (it's single config entry)
            var projectLogoIconUrl = config['projectLogoIconUrl'][0];
            var fullUrl = baseUri.replace("/auth/", "") + resourcesPath + "/" + projectLogoIconUrl;
            var image = createElementFromHTML("<img src='" + fullUrl + "' alt='" + realm + "' style='max-height:100px; width:auto;'>")
            var logoParentDiv = document.querySelector('#kc-header-wrapper');
            logoParentDiv.appendChild(image);

            //set footer icons/logos urls (multiple config entries)
            var iconUrls = config['footerIconUrls'];
            var logosContainerElem = document.querySelector('#footer-logos-container');
            if(iconUrls != null && iconUrls.length > 0){
                for (let i = 0; i < iconUrls.length; i++) {
                    var iconUrl = iconUrls[i];
                    if(iconUrl != null && iconUrl.length > 0){
                        var fullUrl = baseUri.replace("/auth/", "") + resourcesPath + "/" + iconUrl;
                        var logoUrlElem = createElementFromHTML("<img src='" + fullUrl + "' style='max-height:50px; margin: auto;' class='horizontal-padding-10'></img>");
                        logosContainerElem.appendChild(logoUrlElem);
                    }
                }
            }

            //set privacy policy url (it's single config entry)
            var privacyPolicyUrl = config['privacyPolicyUrl'];
            var linksContainerElem = document.querySelector('#footer-links-container');
            if(privacyPolicyUrl != null && privacyPolicyUrl.length > 0 && privacyPolicyUrl[0].length > 0){
                var privacyProlicyElem = createElementFromHTML("<a class='horizontal-padding-10' href='" + privacyPolicyUrl[0] + "'>Privacy</a>");
                linksContainerElem.appendChild(privacyProlicyElem);
            }

            //set terms of use policy url (it's single config entry)
            var termsOfUseUrl = config['termsOfUseUrl'];
            var linksContainerElem = document.querySelector('#footer-links-container');
            var defaultTOUUrl = baseUri + "realms/" + realm + "/theme-info/terms-of-use";
            var termsOfUseElem = createElementFromHTML("<a class='horizontal-padding-10' href='" + defaultTOUUrl + "'>Terms</a>");
            if(termsOfUseUrl != null && termsOfUseUrl.length > 0 && termsOfUseUrl[0].length > 0){
                termsOfUseElem = createElementFromHTML("<a class='horizontal-padding-10' href='" + termsOfUseUrl[0] + "'>Terms</a>");
            }
            linksContainerElem.appendChild(termsOfUseElem);

            //set contact url (it's single config entry)
            var contactUrl = config['contactUrl'];
            var contactContainerElem = document.querySelector('#footer-contact-container');
            if(contactUrl != null && contactUrl.length > 0 && contactUrl[0].length > 0){
                var contactElem = createElementFromHTML("<a class='horizontal-padding-10' href='" + contactUrl[0] + "'>Contact</a>");
                contactContainerElem.appendChild(contactElem);
            }

            //set html footer text (it's single config entry)
            var htmlFooterText = config['htmlFooterText'];
            var footerHtmlTextElem = document.querySelector('#footer-html-text');
            if(htmlFooterText != null && htmlFooterText.length > 0) {
                footerHtmlTextElem.innerHTML = htmlFooterText[0];
            }

            //set a red ribbon if the theme has a ribbon text to show
            var ribbonText = config['ribbonText'];
            if(ribbonText != null && ribbonText.length > 0 && ribbonText[0]) {
                document.body.appendChild(createElementFromHTML("<div class='corner-ribbon'>" + ribbonText + "</div>"));
            }



        }

        function createElementFromHTML(htmlString) {
            var div = document.createElement('div');
            div.innerHTML = htmlString.trim();
            return div.firstChild;
        }


        function drawFooterInPlace(){
            fetch(baseUri.replace("/auth/", "") + resourcesPath + "/elements/footer.html")
                .then((r)=>{r.text().then((d)=>{
                    let element = createElementFromHTML(d);
                    document.getElementsByClassName("login-pf-page")[0].appendChild(element);
                    getConfig();
                })
            });
        }

        function removeDefaultLogo() {
            var logoParentDiv = document.querySelector('#kc-header-wrapper');
            for(var i=0 ; i<logoParentDiv.childNodes.length; i++){
                var child = logoParentDiv.childNodes[i];
                if(child.tagName == undefined || child.tagName != 'IMG')
                    child.remove();
            }
        }



    </script>


</#macro>