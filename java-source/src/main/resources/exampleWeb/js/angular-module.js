"use strict";

// --------
// WARNING:
// --------

// THIS CODE IS ONLY MADE AVAILABLE FOR DEMONSTRATION PURPOSES AND IS NOT SECURE!
// DO NOT USE IN PRODUCTION!

// FOR SECURITY REASONS, USING A JAVASCRIPT WEB APP HOSTED VIA THE CORDA NODE IS
// NOT THE RECOMMENDED WAY TO INTERFACE WITH CORDA NODES! HOWEVER, FOR THIS
// PRE-ALPHA RELEASE IT'S A USEFUL WAY TO EXPERIMENT WITH THE PLATFORM AS IT ALLOWS
// YOU TO QUICKLY BUILD A UI FOR DEMONSTRATION PURPOSES.

// GOING FORWARD WE RECOMMEND IMPLEMENTING A STANDALONE WEB SERVER THAT AUTHORISES
// VIA THE NODE'S RPC INTERFACE. IN THE COMING WEEKS WE'LL WRITE A TUTORIAL ON
// HOW BEST TO DO THIS.

const app = angular.module('demoAppModule', ['ui.bootstrap']);

// Fix for unhandled rejections bug.
app.config(['$qProvider', function ($qProvider) {
    $qProvider.errorOnUnhandledRejections(false);
}]);

app.controller('DemoAppController', function($http, $location, $uibModal) {
    const demoApp = this;

    // We identify the node.
    const apiBaseURL = "/api/NDAexample/";
    let peers = [];
    let ndaStates = [];

    $http.get(apiBaseURL + "me").then((response) => demoApp.thisNode = response.data.me);

    $http.get(apiBaseURL + "peers").then((response) => peers = response.data.peers);

    $http.get(apiBaseURL + "ndaStates").then((response) => ndaStates = response.data);

    demoApp.openModal = () => {
        const modalInstance = $uibModal.open({
            templateUrl: 'demoAppModal.html',
            controller: 'ModalInstanceCtrl',
            controllerAs: 'modalInstance',
            resolve: {
                demoApp: () => demoApp,
                apiBaseURL: () => apiBaseURL,
                peers: () => peers,
                ndaStates: () => ndaStates
            }
        });

        modalInstance.result.then(() => {}, () => {});
    };

    demoApp.getNDAs = () => $http.get(apiBaseURL + "ndas")
        .then((response) => demoApp.ndas = Object.keys(response.data)
            .map((key) => response.data[key].state.data)
            .reverse());

    demoApp.getNDAs();
});

app.controller('ModalInstanceCtrl', function ($http, $location, $uibModalInstance, $uibModal, demoApp, apiBaseURL, peers,ndaStates) {
    const modalInstance = this;

    modalInstance.peers = peers;
    modalInstance.ndaStates = ndaStates;
    modalInstance.form = {};
    modalInstance.formError = false;

    // Validate and create NDA.
    modalInstance.create = () => {
        if (invalidFormInput()) {
            modalInstance.formError = true;
        } else {
            modalInstance.formError = false;

            $uibModalInstance.close();

            const expDate = modalInstance.form.expiryDate;
            //var dateStr = modalInstance.form.expiryDate.getFullYear() + '-' + modalInstance.form.expiryDate.getMonth() + '-' + modalInstance.form.expiryDate.getDate();
            const expDateStr = expDate.getFullYear() + '-' + expDate.getMonth() + '-' + expDate.getDate();

            const stDate = modalInstance.form.startDate;
            const stDateStr =  stDate.getFullYear() + '-' + stDate.getMonth() + '-' + stDate.getDate();

            const counterparty = modalInstance.form.counterparty;
            const terms = modalInstance.form.terms;
            const counterpartyEntity = modalInstance.form.counterpartyEntity;

            const state = modalInstance.form.ndaState;
            const juris = modalInstance.form.jurisdiction;
            const keywords = modalInstance.form.keywords;

            //alert('terms ' + terms);

            const createNDAEndpoint = `${apiBaseURL}create-nda?partyName=${counterparty}&expiryDate=${expDateStr}&partyNameEntity=${counterpartyEntity}&terms=${terms}&startDate=${stDateStr}&state=${state}&juris=${juris}&keywords=${keywords}`;

            // Create PO and handle success / fail responses.
            $http.put(createNDAEndpoint).then(
                (result) => {
                    modalInstance.displayMessage(result);
                    demoApp.getNDAs();
                },
                (result) => {
                    modalInstance.displayMessage(result);
                }
            );
        }
    };

    modalInstance.displayMessage = (message) => {
        const modalInstanceTwo = $uibModal.open({
            templateUrl: 'messageContent.html',
            controller: 'messageCtrl',
            controllerAs: 'modalInstanceTwo',
            resolve: { message: () => message }
        });

        // No behaviour on close / dismiss.
        modalInstanceTwo.result.then(() => {}, () => {});
    };

    // Close create NDA modal dialogue.
    modalInstance.cancel = () => $uibModalInstance.dismiss();

    // Validate the NDA.
    function invalidFormInput() {
        return isNaN(modalInstance.form.expiryDate) || (modalInstance.form.counterparty === undefined)
        || (modalInstance.form.counterpartyEntity === undefined) || modalInstance.form.terms === undefined
        || modalInstance.form.ndaState === undefined;
    }
});

// Controller for success/fail modal dialogue.
app.controller('messageCtrl', function ($uibModalInstance, message) {
    const modalInstanceTwo = this;
    modalInstanceTwo.message = message.data;
});