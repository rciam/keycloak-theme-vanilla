<#import "template.ftl" as layout>

<#-- <script src="${url.resourcesCommonPath}/node_modules/jquery/dist/jquery.min.js" type="text/javascript"></script>  -->
     <script src="${url.resourcesPath}/js/angular.min.js"></script>


    <script>
        var idpLoginFullUrl = '${idpLoginFullUrl?no_esc}';
    </script>


	<script>


		var angularLoginPart = angular.module("angularLoginPart", []);

        angularLoginPart.directive("onScroll", [function () {
            var previousScroll = 0;
            var link = function ($scope, $element, attrs) {
                $element.bind('scroll', function (evt) {
                    var currentScroll = $element.scrollTop;
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

            $scope.disabledLoginButtons = false; // Shared variable to disable all buttons

            $scope.handleLoginClick = function(idp) {
                if (!$scope.disabledLoginButtons) {
                    $scope.disabledLoginButtons = true; // Disable all buttons
                    window.location.href = idp.loginUrl; // Redirect to the login URL
                }
            };

            var sessionParams = new URL(baseUriOrigin+idpLoginFullUrl).searchParams;

            $scope.maxIdPsWithoutSearch = 1;
            $scope.fetchParams = { 'keyword': null, 'first' : 0, 'max': 20, 'client_id': sessionParams.get('client_id'), 'tab_id': sessionParams.get('tab_id'), 'session_code': sessionParams.get('session_code')};
            $scope.idps = [];
            $scope.hiddenIdps = 0;
            $scope.totalIdpsAskedFor = 0;
            $scope.reachedEndPage = false;
            $scope.latestSearch = {};  //for sync purposes
            $scope.isSearching = false;

            function setLoginUrl(idp){
                idp.loginUrl = baseUriOrigin + idpLoginFullUrl.replace("/_/", "/"+idp.alias+"/");
            }

            function getIdps() {
                var submissionTimestamp = new Date().getTime(); //to let the current values be accessible within the callbacks
                var searchParams = $scope.fetchParams; //to let the current values be accessible within the callbacks
                $scope.latestSearch = { submissionTimestamp: submissionTimestamp, searchParams: searchParams };
                $scope.isSearching = true;
                $http({method: 'GET', url: baseUri + '/realms/' + realm + '/theme-info/identity-providers', params : $scope.fetchParams })
                    .then(
                        function(success) {
                            //if we have typed fast multiple chars in the searchbox and the search results delay, this might end up appending duplicate values. prevent it
                            if((searchParams.first == 0) && (submissionTimestamp != $scope.latestSearch.submissionTimestamp)) { //reject the results, there is a newer search
                                return;
                            }
                            $scope.isSearching = false;
                            if(success.data != null && Array.isArray(success.data.identityProviders)){
                                success.data.identityProviders.forEach(function(idp) {
                                    setLoginUrl(idp);
                                    $scope.idps.push(idp);
                                });
                                $scope.hiddenIdps = success.data.hiddenIdps;
                            }
                            else {
                                $scope.reachedEndPage = true;
                            }
                            $scope.totalIdpsAskedFor += $scope.fetchParams.max;
                        },
                        function(error){
                            $scope.isSearching = false;
                        }
                    );
            }

            function getPromotedIdps() {
                $http({method: 'GET', url: baseUri + '/realms/' + realm + '/theme-info/identity-providers-promoted' })
                    .then(
                        function(success) {
                             var response = success.data;
                             $scope.promotedIdps = response.promotedIdPs;
                             $scope.promotedIdps.forEach(function(idp) {
                               setLoginUrl(idp);
                             });
                             $scope.lastLoginIdPs = response.lastLoginIdPs;
                             $scope.lastLoginIdPs.forEach(function(idp) {
                               setLoginUrl(idp);
                             });
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

            $scope.$watch(
                "fetchParams.keyword",
                function handleChange(newValue, oldValue) {
                  if (newValue !== oldValue) {
                    $scope.idps = [];
                    $scope.hiddenIdps = 0;
                    $scope.fetchParams.first = 0;
                    $scope.totalIdpsAskedFor = 0;
                    $scope.reachedEndPage = false;
                    $scope.latestSearch = { timestamp: new Date().getTime(), keyword: newValue };
                    getIdps();
                  }
                }
              );


        });


    </script>
    <style>
            .input-container {
                position: relative;
            }
            .input-container .fa-search {
                position: absolute;
                left: 10px;
                top: 50%;
                transform: translateY(-50%);
                color: #aaa;
            }
            .input-container input {
                width: 100%;
                padding-left: 30px; /* Adjust based on icon size */
                box-sizing: border-box;
            }

            .hr-sect {
                display: flex;
                flex-basis: 100%;
                align-items: center;
                color: rgba(0, 0, 0, 0.35);
                margin: 8px 0px;
            }
            .hr-sect:before,
            .hr-sect:after {
                content: "";
                flex-grow: 1;
                background: rgba(0, 0, 0, 0.35);
                height: 1px;
                font-size: 0px;
                line-height: 0px;
                margin: 0px 8px;
            }
      </style>

<@layout.registrationLayout displayMessage=!messagesPerField.existsError('username','password') displayInfo=realm.password && realm.registrationAllowed && !registrationDisabled??; section>
    <#if section = "header">
        ${msg("loginAccountTitle")}
    <#elseif section = "form">
    <div id="kc-form">
<#--
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
-->


      <div ng-app="angularLoginPart" ng-controller="idpListing">

        <div style="height:0px; position: relative; top: 50%; left: 50%;">
            <img id='spinner' src='${url.resourcesPath}/img/spinner.svg' ng-class="{'hidden' : !isSearching }" style="position: relative; transform: translate(-50%, -50%); width:100px; height:100px;" />
        </div>

        <div ng-if="(idps!=null && idps.length>0) || fetchParams.keyword!=null" id="kc-social-providers" class="${properties.kcFormSocialAccountSectionClass!}">

            <div ng-if="(idps.length >= maxIdPsWithoutSearch && fetchParams.keyword==null) || fetchParams.keyword!=null" class="input-container">
                <i class="kc-social-provider-logo fa fa-search" id="kc-providers-filter-button"> </i>
                <input id="kc-providers-filter" type="text" placeholder="${msg('searchAuthenticationProvider')}" ng-model="fetchParams.keyword" style="padding: 5px 40px;">
            </div>
            <div ng-if="(idps.length < maxIdPsWithoutSearch) || (fetchParams.keyword!=null && fetchParams.keyword!='')">
               <ul id="kc-providers-list" class="${properties.kcFormSocialAccountListClass!} login-pf-list-scrollable" on-scroll="scrollCallback($event, $direct)" >
                  <a ng-repeat="idp in idps" 
                     id="social-{{idp.alias}}" 
                     class="${properties.kcFormSocialAccountListTertiaryButtonClass!}" 
                     ng-class="{ '${properties.kcFormSocialAccountGridItem!}' : idps.length > 3 }" 
                     type="button" 
                     ng-click="handleLoginClick(idp)" 
                     ng-disabled="disabledLoginButtons">
                     <div ng-if="idp.logoUri!=null">
                         <i class="${properties.kcCommonLogoIdP!} fa fa-extend" style="background-size: 100%;background-image: url({{idp.logoUri}});" aria-hidden="true"></i>
                         <span class="${properties.kcFormSocialAccountNameClass!}">{{idp.displayName}}</span>
                     </div>
                     <div ng-if="idp.iconClasses && idp.logoUri==null">
                        <i class="${properties.kcCommonLogoIdP!}" ng-class="{ '{{idp.iconClasses}}' : idp.iconClasses!=null}" aria-hidden="true"></i>
                        <span class="${properties.kcFormSocialAccountNameClass!}">{{idp.displayName}}</span>
                     </div>
                     <div ng-if="!idp.iconClasses && idp.logoUri==null">
                        <span class="${properties.kcFormSocialAccountNameClass!}">{{idp.displayName}}</span>
                     </div>
                  </a>
               </ul>
            </div>

            <div ng-if="promotedIdps.length>0 || lastLoginIdPs.length>0" id="kc-social-providers" class="${properties.kcFormSocialAccountSectionClass!}">
                <div class="hr-sect">
                    <h4>${msg("general-identity-providers")}</h4>
                </div>
                <ul class="${properties.kcFormSocialAccountListClass!} ">
                    <a ng-repeat="idp in lastLoginIdPs" id="social-{{idp.alias}}"
                        class="${properties.kcFormSocialAccountListSecondaryButtonClass!}" ng-class="{ '${properties.kcFormSocialAccountGridItem!}' : promotedIdps.length > 3 }"
                        type="button" ng-click="handleLoginClick(idp)" ng-disabled="isAnyButtonDisabled" 
                        title="${msg('previousLoginProvider')}">
                        <div ng-if="idp.logoUri!=null">
                            <i class="${properties.kcCommonLogoIdP!} fa fa-extend" style="background-size: 100%;background-image: url({{idp.logoUri}});" aria-hidden="true"></i>
                            <span class="${properties.kcFormSocialAccountNameClass!}">{{idp.displayName}}</span>
                        </div>
                        <div ng-if="idp.iconClasses && idp.logoUri==null">
                            <i class="${properties.kcCommonLogoIdP!}" ng-class="{ '{{idp.iconClasses}}' : idp.iconClasses!=null}" aria-hidden="true"></i>
                            <span class="${properties.kcFormSocialAccountNameClass!}">{{idp.displayName}}</span>
                        </div>
                        <div ng-if="!idp.iconClasses && idp.logoUri==null">
                            <span class="${properties.kcFormSocialAccountNameClass!}">{{idp.displayName}}</span>
                        </div>
                    </a>
                    <a ng-repeat="idp in promotedIdps" id="social-{{idp.alias}}" class="${properties.kcFormSocialAccountListTertiaryButtonClass!}" ng-class="{ '${properties.kcFormSocialAccountGridItem!}' : promotedIdps.length > 3 }" type="button" ng-click="handleLoginClick(idp)" ng-disabled="isAnyButtonDisabled">
                        <div ng-if="idp.logoUri!=null">
                           <i class="${properties.kcCommonLogoIdP!} fa fa-extend" style="background-size: 100%;background-image: url({{idp.logoUri}});" aria-hidden="true"></i>
                           <span class="${properties.kcFormSocialAccountNameClass!}">{{idp.displayName}}</span>
                        </div>
                        <div ng-if="idp.iconClasses && idp.logoUri==null">
                            <i class="${properties.kcCommonLogoIdP!}" ng-class="{ '{{idp.iconClasses}}' : idp.iconClasses!=null}" aria-hidden="true"></i>
                            <span class="${properties.kcFormSocialAccountNameClass!}">{{idp.displayName}}</span>
                        </div>
                        <div ng-if="!idp.iconClasses && idp.logoUri==null">
                             <span class="${properties.kcFormSocialAccountNameClass!}">{{idp.displayName}}</span>
                        </div>
                    </a>
                </ul>
            </div>
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
    <div>

</@layout.registrationLayout>