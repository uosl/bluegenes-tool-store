(ns bluegenes-tool-store.core
  (:require [compojure.core :refer [GET defroutes context]]
            [compojure.route :refer [resources files]]
            [config.core :refer [env]]
            [ring.middleware.reload :refer [wrap-reload]]
            [ring.middleware.format :refer [wrap-restful-format]]
            [bluegenes-tool-store.tools :refer [tools]]
            [ring.adapter.jetty :refer [run-jetty]]
            [taoensso.timbre :as timbre :refer [infof errorf]])
  (:gen-class))

(defroutes routes
  ;; serve compiled files, i.e. js, css, from the resources folder
  ;(resources "/")

  ;; serve all tool files in bluegenes/tools automatically.
  ;; they can't go in the resource folder b/c then they get jarred
  ;; when running uberjar or clojar targets,
  ;; and make the jars about a million megabytes too big.
  (files "/tools" {:root (:bluegenes-tool-path env), :allow-symlinks? true})
  (context "/api" []
           (context "/tools" []
                    (GET "/all" [] (tools)))))

(def handler (-> #'routes
                 ; Watch changes to the .clj and hot reload them
                 wrap-reload
                 ; Accept and parse request parameters in various formats
                 (wrap-restful-format :formats
                                      [:json :json-kw :transit-msgpack :transit-json])))

(defn -main
  "Start the BlueGenes Tool Store server.
  This is the main entry point for the application"
  [& args]
  ;; Parse the port from the configuration file, environment variables, or default to 5001
  ;; "PORT" is often the default value for app serving platforms such as Heroku and Dokku
  (let [port (Integer/parseInt (or (:server-port env) (:port env) "5001"))]
    (timbre/set-level! :info) ; Enable Logging
    ;; Start the Jetty server by passing in the URL routes defined in `handler`
    (run-jetty handler {:port port :join? false})
    (infof "=== Bluegenes Tool Server server started on port: %s" port)))
