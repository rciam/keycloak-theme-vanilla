<#import "template.ftl" as layout>

    <script src="${url.resourcesCommonPath}/node_modules/jquery/dist/jquery.min.js" type="text/javascript"></script>
    <script src="${url.resourcesCommonPath}/node_modules/angular/angular.min.js"></script>


    <script>
        var aClass = '${properties.kcFormSocialAccountListButtonClass!}'.split(" ");
        var iconClass = '${properties.kcCommonLogoIdP!}'.split(" ");
        var realm = '${realm.name}';
        var baseUri = '${uriInfo.baseUri}';
        var idpLoginFullUrl = '${idpLoginFullUrl?no_esc}';
    </script>


	<script>

		var angularLoginPart = angular.module("angularLoginPart", []);


        angularLoginPart.directive("onScroll", [function () {
            var previousScroll = 0;
            var link = function ($scope, $element, attrs) {
                $element.bind('scroll', function (evt) {
                    var currentScroll = $element.scrollTop();
                    $scope.$eval(attrs["onScroll"], {$event: evt, $direct: currentScroll > previousScroll ? 1 : -1});
                    previousScroll = currentScroll;
                });
            };
            return {
                restrict: "A",
                link: link
            };
        }]);


		angularLoginPart.controller("idpListing", function($scope, $http) {

            var sessionParams = new URL(baseUri+idpLoginFullUrl).searchParams;

            $scope.fetchParams = { 'keyword': null, 'first' : 0, 'max': 20, 'client_id': sessionParams.get('client_id'), 'tab_id': sessionParams.get('tab_id'), 'session_code': sessionParams.get('session_code')};
            $scope.idps = [];
            $scope.totalIdpsAskedFor = 0;
            $scope.reachedEndPage = false;

            function setLoginUrl(idp){
                idp.loginUrl = baseUri.replace("/auth/", "") + idpLoginFullUrl.replace("/_/", "/"+idp.alias+"/");
            }

            function getIdps() {
                $http({method: 'GET', url: baseUri + 'realms/' + realm + '/identity-providers', params : $scope.fetchParams })
                    .then(
                        function(success) {
                            if(Array.isArray(success.data)) {
                                success.data.forEach(function(idp) {
                                    setLoginUrl(idp);
                                    $scope.idps.push(idp);
                                });
                            }
                            else {
                                $scope.reachedEndPage = true;
                            }
                            $scope.totalIdpsAskedFor += $scope.fetchParams.max;
                        },
                        function(error){
                        }
                    );
            }

            function getPromotedIdps() {
                $http({method: 'GET', url: baseUri + 'realms/' + realm + '/identity-providers-promoted' })
                    .then(
                        function(success) {
                            success.data.forEach(function(idp) {
                                setLoginUrl(idp);
                            });
                            $scope.promotedIdps = success.data;
                        },
                        function(error){
                        }
                    );
            }


            getIdps();

            getPromotedIdps();



            $scope.scrollCallback = function ($event, $direct) {
                if($scope.reachedEndPage==true || $event.target.lastElementChild==null)
                    return;
                if(($event.target.scrollTop + $event.target.clientHeight) > ($event.target.scrollHeight - $event.target.lastElementChild.clientHeight)){
                    if($scope.totalIdpsAskedFor < $scope.fetchParams.first + $scope.fetchParams.max){ //means that there is an ongoing fetching or reached the end
                        console.log("loading or reached end of stream");
                    }
                    else{
                        $scope.fetchParams.first += $scope.fetchParams.max;
                        getIdps();
                    }
                }

            };

            $scope.applySearch = function(e){
                if(e==null || e.which === 13){ //from button click or from keyboard key===enter
                    $scope.idps = [];
                    $scope.fetchParams.first = 0;
                    $scope.totalIdpsAskedFor = 0;
                    $scope.reachedEndPage = false;
                    getIdps();
                }
            }

        });


    </script>


<@layout.registrationLayout displayMessage=!messagesPerField.existsError('username','password') displayInfo=realm.password && realm.registrationAllowed && !registrationDisabled??; section>

    <#if section = "header">
        ${msg("loginAccountTitle")}
    <#elseif section = "form">
    <div id="kc-form">
      <div id="kc-form-wrapper">
        <#if realm.password>
            <form id="kc-form-login" onsubmit="login.disabled = true; return true;" action="${url.loginAction}" method="post">
                <div class="${properties.kcFormGroupClass!}">
                    <label for="username" class="${properties.kcLabelClass!}"><#if !realm.loginWithEmailAllowed>${msg("username")}<#elseif !realm.registrationEmailAsUsername>${msg("usernameOrEmail")}<#else>${msg("email")}</#if></label>

                    <#if usernameEditDisabled??>
                        <input tabindex="1" id="username" class="${properties.kcInputClass!}" name="username" value="${(login.username!'')}" type="text" disabled />
                    <#else>
                        <input tabindex="1" id="username" class="${properties.kcInputClass!}" name="username" value="${(login.username!'')}"  type="text" autofocus autocomplete="off"
                               aria-invalid="<#if messagesPerField.existsError('username','password')>true</#if>"
                        />

                        <#if messagesPerField.existsError('username','password')>
                            <span id="input-error" class="${properties.kcInputErrorMessageClass!}" aria-live="polite">
                                    ${kcSanitize(messagesPerField.getFirstError('username','password'))?no_esc}
                            </span>
                        </#if>
                    </#if>
                </div>

                <div class="${properties.kcFormGroupClass!}">
                    <label for="password" class="${properties.kcLabelClass!}">${msg("password")}</label>

                    <input tabindex="2" id="password" class="${properties.kcInputClass!}" name="password" type="password" autocomplete="off"
                           aria-invalid="<#if messagesPerField.existsError('username','password')>true</#if>"
                    />
                </div>

                <div class="${properties.kcFormGroupClass!} ${properties.kcFormSettingClass!}">
                    <div id="kc-form-options">
                        <#if realm.rememberMe && !usernameEditDisabled??>
                            <div class="checkbox">
                                <label>
                                    <#if login.rememberMe??>
                                        <input tabindex="3" id="rememberMe" name="rememberMe" type="checkbox" checked> ${msg("rememberMe")}
                                    <#else>
                                        <input tabindex="3" id="rememberMe" name="rememberMe" type="checkbox"> ${msg("rememberMe")}
                                    </#if>
                                </label>
                            </div>
                        </#if>
                        </div>
                        <div class="${properties.kcFormOptionsWrapperClass!}">
                            <#if realm.resetPasswordAllowed>
                                <span><a tabindex="5" href="${url.loginResetCredentialsUrl}">${msg("doForgotPassword")}</a></span>
                            </#if>
                        </div>

                  </div>

                  <div id="kc-form-buttons" class="${properties.kcFormGroupClass!}">
                      <input type="hidden" id="id-hidden-input" name="credentialId" <#if auth.selectedCredential?has_content>value="${auth.selectedCredential}"</#if>/>
                      <input tabindex="4" class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!} ${properties.kcButtonLargeClass!}" name="login" id="kc-login" type="submit" value="${msg("doLogIn")}"/>
                  </div>
            </form>
        </#if>
      </div>



      <div ng-app="angularLoginPart" ng-controller="idpListing">

        <div ng-if="promotedIdps!=null && promotedIdps.length>0" id="kc-social-providers" class="${properties.kcFormSocialAccountSectionClass!}">
            <hr/>
            <h4>${msg("general-identity-providers")}</h4>
            <ul class="${properties.kcFormSocialAccountListClass!} ">
                <a ng-repeat="idp in promotedIdps" id="social-{{idp.alias}}" class="${properties.kcFormSocialAccountListButtonClass!}" ng-class="{ '${properties.kcFormSocialAccountGridItem!}' : promotedIdps.length > 3 }" type="button" href="{{idp.loginUrl}}">
                    <div ng-if="idp.iconClasses!=null">
                        <i class="${properties.kcCommonLogoIdP!}" ng-class="{ '{{idp.iconClasses}}' : idp.iconClasses!=null}" aria-hidden="true"></i>
                        <span class="${properties.kcFormSocialAccountNameClass!} kc-social-icon-text">{{idp.displayName}}</span>
                    </div>
                    <div ng-if="idp.iconClasses==null">
                        <span class="${properties.kcFormSocialAccountNameClass!}">{{idp.displayName}}</span>
                    </div>
                </a>
            </ul>
        </div>
        <div ng-if="(idps!=null && idps.length>0) || fetchParams.keyword!=null" id="kc-social-providers" class="${properties.kcFormSocialAccountSectionClass!}">
            <hr/>
            <h4>${msg("identity-provider-login-label")}</h4>

            <div ng-if="(idps.length>=fetchParams.max && fetchParams.keyword==null) || fetchParams.keyword!=null">
                <input id="kc-providers-filter" type="text" placeholder="Search..." ng-model="fetchParams.keyword" ng-keypress="applySearch($event)">
                <i class="fa fa-search" id="kc-providers-filter-button" data-ng-click="applySearch(null)"> </i>
            </div>
            <ul id="kc-providers-list" class="${properties.kcFormSocialAccountListClass!} login-pf-list-scrollable" on-scroll="scrollCallback($event, $direct)" >
               <a ng-repeat="idp in idps" id="social-{{idp.alias}}" class="${properties.kcFormSocialAccountListButtonClass!}" ng-class="{ '${properties.kcFormSocialAccountGridItem!}' : idps.length > 3 }" type="button" href="{{idp.loginUrl}}">
                  <div ng-if="idp.iconClasses!=null">
                     <i class="${properties.kcCommonLogoIdP!}" ng-class="{ '{{idp.iconClasses}}' : idp.iconClasses!=null}" aria-hidden="true"></i>
                     <span class="${properties.kcFormSocialAccountNameClass!} kc-social-icon-text">{{idp.displayName}}</span>
                  </div>
                  <div ng-if="idp.iconClasses==null">
                     <span class="${properties.kcFormSocialAccountNameClass!}">{{idp.displayName}}</span>
                  </div>
               </a>
            </ul>
        </div>
      </div>

    </div>
    <#elseif section = "info" >
        <#if realm.password && realm.registrationAllowed && !registrationDisabled??>
            <div id="kc-registration-container">
                <div id="kc-registration">
                    <span>${msg("noAccount")} <a tabindex="6" href="${url.registrationUrl}">${msg("doRegister")}</a></span>
                </div>
            </div>
        </#if>
    </#if>

</@layout.registrationLayout>