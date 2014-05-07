(ns webapp.client.components.playback
  (:require
   [goog.net.cookies :as cookie]
   [om.core          :as om :include-macros true]
   [om.dom           :as dom :include-macros true]
   [cljs.core.async  :refer [put! chan <! pub timeout]]
   [om-sync.core     :as async]
   [clojure.data     :as data]
   [clojure.string   :as string]
   [ankha.core       :as ankha])

  (:use
   [webapp.framework.client.coreclient :only  [log remote]]
   [webapp.client.globals              :only  [app-state   playback-app-state
                                               playback-controls-state
                                               reset-app-state
                                               reset-playback-app-state]]
   [webapp.client.components.views     :only  [main-view]]
   [webapp.client.components.forms     :only  [request-form]]
   )
  (:use-macros
   [webapp.framework.client.neo4j      :only  [neo4j]]
   )
  (:require-macros
   [cljs.core.async.macros :refer [go]]))







(def playbacktime (atom 0))

(defn playback-session [& {:keys
                           [session-id]}]
  (go
   (reset! playback-controls-state
           (update-in @playback-controls-state
                      [:ui :current-session]
                      (fn[_] session-id)))
   (let [
            init-state  (<! (neo4j "match (n:WebSession) where
                                   n.session_id={session_id}
                                   return n.init_state"
                                   {:session_id    session-id}))


            ll          (<! (neo4j "
                                   match (r:WebRecord) where
                                   r.session_id={session_id}
                                   return r order by r.seq_ord
                                   "
                                   {:session_id    session-id}
                                   "r"))]

        (om/root
         main-view
         playback-app-state
         {:target (js/document.getElementById "playback_canvas")})


        (reset! playback-app-state (cljs.reader/read-string init-state))
        ;(log (str "sssss=" init-state))
        (doseq [item ll]
          (let [
                path      (cljs.reader/read-string (:path (into {} item )))
                content   (cljs.reader/read-string (:new_value (into {} item )))
                timestamp   (:timestamp (into {} item ))
                ]
            ;(log path)
            ;(log content)
            ;(log timestamp )
            (<! (timeout (-  timestamp @playbacktime)))
            (reset! playbacktime timestamp)
            (reset! playback-app-state
                    (update-in @playback-app-state
                               path
                               (fn[_] content)))

            nil
            )
          )
        (js/alert "session finished")
        ))
  )








(defn replay-session [session-id]
  (go
   (let [
         ll          (<! (neo4j "match (n:WebSession) return n.session_id"
                        {} "n.session_id"))
        ]

     (reset! playback-controls-state (assoc-in
                                      @playback-controls-state
                                      [:data :sessions]  (into [](take 5 ll))))

     (om/root
      main-view
      playback-app-state
      {:target (js/document.getElementById "playback_canvas")}))))









(defn playback-session-button-component [{:keys [ui
                                                 data
                                                 sessions
                                                 current-session]} owner]
  (reify

    om/IRenderState
    ;---------

    (render-state
     [this {:keys [highlight unhighlight]}]
      (let [
              session-id   (get-in data ["session_id"])
              browser      (get-in data ["browser"])
              start-time   (js/Date. (get-in data ["start_time"]))
              full-time    (/ (get-in data ["time"]) 1000)
              active       (= session-id current-session)
            ]
       (dom/div nil

              (dom/div
               #js {
                    :style      #js {:padding-top "40px"
                                     :backgroundColor
                                     (if
                                       (= (get-in ui
                                               [:sessions session-id :highlighted])
                                          "true")
                                       "darkgray"
                                       (if active
                                         "lightgray"
                                         "white")
                                       )
                                     }

                    :onClick
                      (fn [e]  (playback-session  :session-id  session-id))

                    :onMouseEnter
                      (fn[e]   (put! highlight    session-id))
                    :onTouchStart
                      (fn[e]   (put! highlight    session-id))

                    :onMouseLeave
                      (fn[e]   (put! unhighlight  session-id))
                    :onTouchEnd
                      (fn[e]   (put! highlight  session-id))
                    :onTouchMove
                      (fn[e]   (put! highlight  session-id))
                    }
               (str browser "  :::  " start-time  ", "
                    full-time " secs")))))))















(defn playback-controls-view [app owner]

  (reify

    om/IInitState
    ;------------

    (init-state [_]

                {
                   :highlight                (chan)
                   :unhighlight              (chan)
                   :show-ankha1              (chan)
                   :clear-replay-sessions    (chan)
                   :show-ankha               (chan)
                })

    om/IWillMount
    ;------------
    (will-mount [_]
                (let [
                      highlight               (om/get-state owner :highlight)
                      unhighlight             (om/get-state owner :unhighlight)
                      clear-replay-sessions   (om/get-state owner :clear-replay-sessions)
                      show-ankha              (om/get-state owner :show-ankha)
                      show-ankha1             (om/get-state owner :show-ankha1)
                      ]
                  (go (loop []
                        (let [session (<! highlight)]
                          ;(log "****HIGHLIGHT")
                          (om/transact!
                           app
                           [:ui :sessions session :highlighted] (fn[x] "true" ))
                          (recur))))

                  (go (loop []
                        (let [session (<! unhighlight)]
                          ;(log "****UNHIGHLIGHT")
                          (om/transact!
                           app
                           [:ui :sessions session :highlighted] (fn[x] "false" ))
                          (recur))))
                  (go (loop []
                        (let [session (<! clear-replay-sessions)]
                          (log "****CLEAR REPLAY")
                          (om/transact!
                           app
                           [:data :sessions] (fn[x] {} ))
                          (<! (remote "clear-playback-sessions" {}))
                          (recur))))

                  (go (loop []
                        (let [session (<! show-ankha)]
                          (log "****SHOW ANKHA")
                          (om/root
                            ankha/inspector
                            playback-app-state
                            {:target (js/document.getElementById "playback_state")})

                          (recur))))
                  (go (loop []
                        (let [session (<! show-ankha1)]
                          (log "****SHOW ANKHA1")
                          (om/root
                            ankha/inspector
                            playback-controls-state
                            {:target (js/document.getElementById "playback_ankha")})

                          (recur))))



                  ))

    om/IRenderState
    ;--------------

    (render-state
     [this {:keys [highlight
                   unhighlight
                   clear-replay-sessions
                   show-ankha
                   show-ankha1]}]
     (comment log (str "map="(mapv
                                      (fn [x]
                                        {
                                        :ui      (-> app :ui)
                                        :data    x
                                        }
                                        )
                                      (-> app :data :sessions))))

     (dom/div nil
              (dom/h2 nil "Playback web sessions")


              (apply dom/ul nil
                     (om/build-all  playback-session-button-component
                                    (mapv
                                     (fn [x]
                                       {
                                        :ui         (-> app :ui)
                                        :sessions   (-> app :data :sessions)
                                        :current-session   (-> app :ui :current-session)
                                        :data       x
                                        }
                                       )
                                     (-> app :data :sessions)

                                     )
                                    {:init-state {:highlight    highlight
                                                  :unhighlight  unhighlight}}
                                    )
                     )

              (dom/button #js {:onClick (fn [e] (put! clear-replay-sessions
                                                      true))} "Delete")
              (dom/button #js {:onClick (fn [e] (put! show-ankha
                                                      true))} "Ankha")
              (dom/button #js {:onClick (fn [e] (put! show-ankha1
                                                      true))} "Ankha1")


              ))))
