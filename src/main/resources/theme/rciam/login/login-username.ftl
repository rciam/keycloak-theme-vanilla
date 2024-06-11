<#import "template.ftl" as layout>

<#-- <script src="${url.resourcesCommonPath}/node_modules/jquery/dist/jquery.min.js" type="text/javascript"></script>  -->
    <script src="${url.resourcesCommonPath}/node_modules/angular/angular.min.js"></script>

    <script>
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

            var sessionParams = new URL(baseUriOrigin+idpLoginFullUrl).searchParams;

            $scope.maxIdPsWithoutSearch = 6;
            $scope.fetchParams = { 'keyword': null, 'first' : 0, 'max': 20, 'client_id': sessionParams.get('client_id'), 'tab_id': sessionParams.get('tab_id'), 'session_code': sessionParams.get('session_code')};
            $scope.idps = [];
            $scope.totalIdpsAskedFor = 0;
            $scope.reachedEndPage = false;

            function setLoginUrl(idp){
                idp.loginUrl = baseUriOrigin + idpLoginFullUrl.replace("/_/", "/"+idp.alias+"/");
            }

            function getIdps() {
                $http({method: 'GET', url: baseUri + '/realms/' + realm + '/theme-info/identity-providers', params : $scope.fetchParams })
                    .then(
                        function(success) {
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
                        }
                    );
            }

            function getPromotedIdps() {
                $http({method: 'GET', url: baseUri + '/realms/' + realm + '/theme-info/identity-providers-promoted' })
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

            $scope.$watch(
                "fetchParams.keyword",
                function handleChange(newValue, oldValue) {
                  if (newValue !== oldValue) {
                    $scope.idps = [];
                    $scope.hiddenIdps = 0;
                    $scope.fetchParams.first = 0;
                    $scope.totalIdpsAskedFor = 0;
                    $scope.reachedEndPage = false;
                    getIdps();
                  }
                }
              );


        });


    </script>

<@layout.registrationLayout displayMessage=!messagesPerField.existsError('username') displayInfo=(realm.password && realm.registrationAllowed && !registrationDisabled??); section>
    <#if section = "header">
        ${msg("loginAccountTitle")}
    <#elseif section = "form">
        <div id="kc-form">
            <div id="kc-form-wrapper">
                <#if realm.password>
                    <form id="kc-form-login" onsubmit="login.disabled = true; return true;" action="${url.loginAction}"
                          method="post">
                        <div class="${properties.kcFormGroupClass!}">
                            <label for="username"
                                   class="${properties.kcLabelClass!}"><#if !realm.loginWithEmailAllowed>${msg("username")}<#elseif !realm.registrationEmailAsUsername>${msg("usernameOrEmail")}<#else>${msg("email")}</#if></label>

                            <#if usernameEditDisabled??>
                                <input tabindex="1" id="username"
                                       aria-invalid="<#if message?has_content && message.type = 'error'>true</#if>"
                                       class="${properties.kcInputClass!}" name="username"
                                       value="${(login.username!'')}"
                                       type="text" disabled/>
                            <#else>
                                <input tabindex="1" id="username"
                                       aria-invalid="<#if messagesPerField.existsError('username')>true</#if>"
                                       class="${properties.kcInputClass!}" name="username"
                                       value="${(login.username!'')}"
                                       type="text" autofocus autocomplete="off"/>
                            </#if>

                            <#if messagesPerField.existsError('username')>
                                <span id="input-error-username" class="${properties.kcInputErrorMessageClass!}" aria-live="polite">
                                    ${kcSanitize(messagesPerField.get('username'))?no_esc}
                                </span>
                            </#if>
                        </div>

                        <div class="${properties.kcFormGroupClass!} ${properties.kcFormSettingClass!}">
                            <div id="kc-form-options">
                                <#if realm.rememberMe && !usernameEditDisabled??>
                                    <div class="checkbox">
                                        <label>
                                            <#if login.rememberMe??>
                                                <input tabindex="3" id="rememberMe" name="rememberMe" type="checkbox"
                                                       checked> ${msg("rememberMe")}
                                            <#else>
                                                <input tabindex="3" id="rememberMe" name="rememberMe"
                                                       type="checkbox"> ${msg("rememberMe")}
                                            </#if>
                                        </label>
                                    </div>
                                </#if>
                            </div>
                        </div>

                        <div id="kc-form-buttons" class="${properties.kcFormGroupClass!}">
                            <input tabindex="4"
                                   class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!} ${properties.kcButtonLargeClass!}"
                                   name="login" id="kc-login" type="submit" value="${msg("doLogIn")}"/>
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
                            <span class="${properties.kcFormSocialAccountNameClass!}">{{idp.displayName}}</span>
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

                <div ng-if="(idps.length >= maxIdPsWithoutSearch && fetchParams.keyword==null) || fetchParams.keyword!=null">
                    <i class="fa fa-search" id="kc-providers-filter-button"> </i>
                    <input id="kc-providers-filter" type="text" placeholder="Search your authentication provider" ng-model="fetchParams.keyword" ng-keypress="applySearch($event)" style="width:80%">
                </div>
                <div ng-if="(idps.length < maxIdPsWithoutSearch) || (fetchParams.keyword!=null && fetchParams.keyword!='')">
                   <ul id="kc-providers-list" class="${properties.kcFormSocialAccountListClass!} login-pf-list-scrollable" on-scroll="scrollCallback($event, $direct)" >
                      <a ng-repeat="idp in idps" id="social-{{idp.alias}}" class="${properties.kcFormSocialAccountListButtonClass!}" ng-class="{ '${properties.kcFormSocialAccountGridItem!}' : idps.length > 3 }" type="button" href="{{idp.loginUrl}}">
                         <div ng-if="idp.iconClasses!=null">
                            <i class="${properties.kcCommonLogoIdP!}" ng-class="{ '{{idp.iconClasses}}' : idp.iconClasses!=null}" aria-hidden="true"></i>
                            <span class="${properties.kcFormSocialAccountNameClass!}">{{idp.displayName}}</span>
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
            <div id="kc-registration">
                <span>${msg("noAccount")} <a tabindex="6" href="${url.registrationUrl}">${msg("doRegister")}</a></span>
            </div>
        </#if>
    </#if>

</@layout.registrationLayout>
