(ns sample3.core
  (:require
    [day8.re-frame.http-fx]
    [reagent.dom :as rdom]
    [reagent.core :as r]
    [re-frame.core :as rf]
    [goog.events :as events]
    [goog.history.EventType :as HistoryEventType]
    [markdown.core :refer [md->html]]
    [sample3.ajax :as ajax]
    [ajax.core :refer [GET POST]]
    [sample3.events]
    [reitit.core :as reitit]
    [reitit.frontend.easy :as rfe]
    [clojure.string :as string])
  (:import goog.History))

(defn nav-link [uri title page]
  [:a.navbar-item
   {:href   uri
    :class (when (= page @(rf/subscribe [:common/page-id])) :is-active)}
   title])

(defn navbar [] 
  (r/with-let [expanded? (r/atom false)]
              [:nav.navbar.is-info>div.container
               [:div.navbar-brand
                [:a.navbar-item {:href "/" :style {:font-weight :bold}} "sample3"]
                [:span.navbar-burger.burger
                 {:data-target :nav-menu
                  :on-click #(swap! expanded? not)
                  :class (when @expanded? :is-active)}
                 [:span][:span][:span]]]
               [:div#nav-menu.navbar-menu
                {:class (when @expanded? :is-active)}
                [:div.navbar-start
                 [nav-link "#/" "Home" :home]
                 [nav-link "#/about" "About" :about]]]]))

(defn about-page []
  [:section.section>div.container>div.content
   [:img {:src "/img/warning_clojure.png"}]])

(defn postSum [data]
      (POST "/api/math/plus"    {:headers {"accept" "application/json"}
                                 :params {:x (:x @data) :y (:y @data)}
                                 :handler #(swap! data assoc :result (:total %))})
      )

(defn getSum [vec resultAtom]
      (GET "/api/math/plus"    {:headers {"accept" "application/json"}
                                :params {:x (first vec) :y (second vec)}
                                :handler #(reset! resultAtom (:total %))}

           )
      )

(defn postProduct [data]
      (POST "/api/math/times" {:headers {"accept" "application/json"}
                               :params  {:x (:x @data) :y (:y @data)}
                               :handler #(swap! data assoc :result (:total %))}))

(defn postDifference [data]
      (POST "/api/math/minus" {:headers {"accept" "application/json"}
                               :params  {:x (:x @data) :y (:y @data)}
                               :handler #(swap! data assoc :result (:total %))}))

(defn postQuotient [data]
      (POST "/api/math/divide" {:headers {"accept" "application/json"}
                                :params  {:x (:x @data) :y (:y @data)}
                                :handler #(swap! data assoc :result (:total %))}))

(def app-db (r/atom {:x 0 :y 0 :result 0}))

(rf/reg-event-db
  :x-change
  (fn [db [_ x-value]]
   (assoc db :x x-value)))
(rf/reg-event-db
  :y-change
  (fn [db [_ y-value]]
    (assoc db :y y-value)))
(rf/reg-sub
  :x
  (fn [db _]
    (:x db)))
(rf/reg-sub
  :y
  (fn [db _]
    (:y db)))

(defn x-input []
  [:div.field
   [:input.input
    {:type :number
     :value @(rf/subscribe [:x])
     :on-change #(rf/dispatch [:x-change (-> % .-target .-value)])}]])

(defn y-input []
  [:div.field
   [:input.input
    {:type :number
     :value @(rf/subscribe [:y])
     :on-change #(rf/dispatch [:y-change (-> % .-target .-value)])}]])



(defn text-field [tag id data] ;function for input text element. Tag should be :input.input, id is the keyword to access data (:x or :y)
      [:div.field
       [tag
        {:type :number
         :value (id @data)
         :on-change #(do
                       (prn "change" id (-> % .-target .-value))
                       (swap! data assoc id (js/parseInt (-> % .-target .-value)))
                       (postSum data))}]]
      )

(defn genButton [data action label color]
      [:a.button.is-primary  {:on-click #(action data) :style {:background-color color}} label ])

(defn generateMathUI []
      (let [testData (r/atom {:x 0 :y 0 :result 0})]
           (fn []
               [:div
                [:p "Enter First Number:"]
                [text-field :input.input :x testData ]
                [:p]
                [:p "Enter Second Number:"]
                [text-field :input.input :y testData]
                ;[:a.radio {:on-click #(postSum testData)} "+"]
                ;[:a.radio {:on-click #(postProduct testData)} "*"]
                ;[:a.radio {:on-click #(postDifference testData)} "-"]
                ;[:a.radio {:on-click #(postQuotient testData)} "/"]

                (genButton testData postSum "+" "red")
                (genButton testData postDifference "-" "blue")
                (genButton testData postProduct "*" "green")
                (genButton testData postQuotient "/" "orange")


                ;[:p [:label [:input {:type :radio :on-click #(postSum testData)}] "Add"]]
                ;[:p [:label [:input {:type :radio :on-click #(postDifference testData)}] "Subtract"]]
                ;[:p [:label [:input {:type :radio :on-click #(postProduct testData)}] "Multiply"]]
                ;[:p [:label [:input {:type :radio :on-click #(postQuotient testData)}] "Divide"]]

                ;[:p "The result is: " (:result @testData)]
                [:p ;(cond
                 ;(< (:result @testData) 20) {:style {:background-color "LightGreen"}}
                 ;(and (< 19 (:result @testData)) (< (:result @testData) 50) ) {:style {:background-color "LightBlue"}}
                 ;(< 49 (:result @testData)) {:style {:background-color "LightSalmon"}})

                 "The result is: " (:result @testData)]
                ]
               ))
      )

(defn home-page []
  ;[:section.section>div.container>div.content
  ; (when-let [docs @(rf/subscribe [:docs])]
  ;   [:div {:dangerouslySetInnerHTML {:__html (md->html docs)}}])]
  [:div
   [:p "Enter First Value:"]
   [x-input]
   [:p "Enter Second Value:"]
  [y-input]]
  )

(defn page []
  (if-let [page @(rf/subscribe [:common/page])]
    [:div
     [navbar]
     [page]]))

(defn navigate! [match _]
  (rf/dispatch [:common/navigate match]))

(def router
  (reitit/router
    [["/" {:name        :home
           :view        #'home-page
           :controllers [{:start (fn [_] (rf/dispatch [:page/init-home]))}]}]
     ["/about" {:name :about
                :view #'about-page}]]))

(defn start-router! []
  (rfe/start!
    router
    navigate!
    {}))

;; -------------------------
;; Initialize app
(defn ^:dev/after-load mount-components []
  (rf/clear-subscription-cache!)
  (rdom/render [#'page] (.getElementById js/document "app")))

(defn init! []
  (start-router!)
  (ajax/load-interceptors!)
  (mount-components))
