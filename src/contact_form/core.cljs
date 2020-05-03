(ns contact-form.core
  (:require [reagent.core :as reagent :refer [atom]]
            [reagent-forms.core :as forms]
            [cljs.core.async :refer [<!]]
            [cljs-http.client :as http])
  (:require-macros [cljs.core.async.macros :refer [go]]))


(defn is-valid-name?
  "Returns `true` if string `name` is longer than 3 characters."
  [name]
  (>= (count name ) 3))

(defn is-valid-email?
  "Returns `true` if given `email` is of type `str` and matches email regex."
  [email]
(let [pattern #"[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?"]
  (if (and (string? email) (re-matches pattern email)) true false)))


;; lambda endpoint to send email
;; TODO change to prod endpoint before deploying to Prod
(def endpoint "https://bnodafauxe.execute-api.eu-central-1.amazonaws.com/Alpha/dev")

(defn send-mail
  "Sends a get request to Lambda to send email based on `doc` atom
  that represent the contact form fields. Alerts the user based
  on the request result.

  |key        | description             |
  |-----------|-------------------------|
  |`:name`    | name input field        |
  |`:email`   | email input field       |
  |`:phone`   | phone input field       |
  |`:message` | message input field     |"

  [doc]
  (go (let [response (<! (http/get endpoint {:with-credentials? false
                                             :query-params doc}))]
        (if (= 200 (:status response))
          (js/alert "Ευχαριστούμε για το μήνυμα σας. Θα επικοινωνίσουμε σύντομα μαζί σας")
          (js/alert "Σφάλμα κατά την αποστολή δεδομένων, παρακαλώ επικοινωνίστε μαζί μας τηλεφωνικώς")))))


;; main body of contact form
;; https://github.com/reagent-project/reagent-forms#binding-the-form-to-a-document
(def form-template
  [:form

   [:div.field
    [:div.field-body
     [:div.field.is-expanded
      [:div.field
       [:label.label "Όνομα"]
       [:div.control.has-icons-right
        [:input.input.is-info {:field :text :id :name :placeholder "όνομα...."
                               :validator (fn [doc]  ;; returns a class "is-success", for input div, if fn evaluates to true
                                            (when
                                                (-> doc :name is-valid-name?)
                                      ["is-success"]))}]
        [:span.icon.is-small.is-right [:i.fas.fa-envelope]]]
       [:p.help.is-danger {:field :alert :id :name :event (fn [name]  ;; renders warning paragraph when evaluated to true
                                                            (not (is-valid-name? name)))}
        "* Υποχρεωτικό πεδίο"]]]]]

   [:div.field
    [:div.field-body
     [:div.field.is-expanded
      [:div.field
       [:label.label "Emai"]
       [:div.control
        [:input.input.is-info {:field :email :id :email :placeholder "email...."
                               :validator (fn [doc]
                                    (when (-> doc :email is-valid-email?)  ;; instead of empty? not which expands to (not (not (seq :email)))
                                      ["is-success"]))}]]
       [:p.help.is-danger {:field :alert :id :email :event (fn [email] (not (is-valid-email? email)))}
        "* Μη έγκυρο email"]]]]]

   [:div.field
    [:div.field-body
     [:div.field.is-expanded
      [:div.field
       [:label.label "Τηλέφωνο"]
       [:div.control
        [:input.input {:field :text :id :phone :placeholder "τηλέφωνο...."
                       :validator (fn [doc]
                                    (when (-> doc :phone empty?)
                                      [""]))}]]]]]]

   [:div.field.is-horizontal
    [:div.field-body
     [:div.field.is-expanded
      [:div.field
       [:label.label "Μήνυμα"]
       [:div.control
        [:textarea.textarea.is-info {:field :text :id :message :placeholder "το μήνυμα σας..."
                             :validator (fn [doc]
                                          (when (-> doc :message seq)
                                            ["is-success"]))}]]
       [:p.help.is-danger {:field :alert :id :message :event empty?}
        "* Υποχρεωτικό πεδίο..."]]]]]])


(defn form
  []
  (let [doc (atom {})]
    (fn []
      [:div
       [forms/bind-fields form-template doc]
       [:div [:br]
        [:button.button.is-primary {:disabled (or (not (is-valid-name? (:name @doc)))  ;; set disabled to true based on state validation
                                                  (not (is-valid-email? (:email @doc)))
                                                  (empty? (:message @doc)))
                                    :on-click #(do (send-mail @doc)
                                                   (reset! doc {}))} "Submit"]]])))


(defn start []
  (reagent/render-component [form]
                            (. js/document (getElementById "app"))))

(defn ^:export init []
  ;; init is called ONCE when the page loads
  ;; this is called in the index.html and must be exported
  ;; so it is available even in :advanced release builds
  (start))

(defn stop []
  ;; stop is called before any code is reloaded
  ;; this is controlled by :before-load in the config
  (js/console.log "stop"))


