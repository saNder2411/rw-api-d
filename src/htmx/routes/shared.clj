(ns htmx.routes.shared
  (:require [hiccup2.core :as h]
            [hiccup.page :as hp]))

(defn ok [body]
  {:status  200
   :headers {"Content-Type" "text/html"}
   :body    (-> body
                (h/html)
                (str))})

(defn layout [title scripts body]
  [:head
   [:title title]
   (apply hp/include-js scripts )
   [:body
    [:div {:class "container mx-auto mt-10"}
     [:h1 {:class "text-2xl font-bold leading-7 text-gray-900 mb-5 sm:p-0 p-6"} title]
     body]]])

(defn random-picture []
  (let [skin-color ["Tanned" "Yellow" "Pale" "Light" "Brown" "DarkBrown" "Black"]
        top-type #{"Hat"
                   "LongHairNotTooLong"
                   "ShortHairDreads01"
                   "ShortHairShortFlat"
                   "ShortHairDreads02"
                   "LongHairFrida"
                   "WinterHat4"
                   "Turban"
                   "LongHairShavedSides"
                   "ShortHairTheCaesarSidePart"
                   "ShortHairShaggyMullet"
                   "ShortHairShortWaved"
                   "ShortHairFrizzle"
                   "LongHairMiaWallace"
                   "WinterHat2"
                   "LongHairBigHair"
                   "Hijab"
                   "LongHairStraightStrand"
                   "LongHairFroBand"
                   "ShortHairSides"
                   "NoHair"
                   "ShortHairShortRound"
                   "WinterHat1"
                   "LongHairDreads"
                   "ShortHairTheCaesar"
                   "LongHairFro"
                   "LongHairBun"
                   "WinterHat3"
                   "LongHairCurvy"
                   "Eyepatch"
                   "LongHairStraight"
                   "LongHairStraight2"
                   "ShortHairShortCurly"
                   "LongHairBob"
                   "LongHairCurly"}
        mouth-type #{"Tongue" "Default" "Smile" "Grimace" "Twinkle" "Disbelief" "Eating" "Sad" "Serious" "Concerned" "ScreamOpen" "Vomit"}
        hair-color #{"Platinum" "Black" "BlondeGolden" "BrownDark" "SilverGray" "Blue" "Brown" "Blonde" "Red" "PastelPink" "Auburn"}]
    (format "https://avataaars.io/?skinColor=%s&topType=%s&hairColor=%s&mouthType=%s"
            (first (shuffle skin-color))
            (first (shuffle top-type))
            (first (shuffle hair-color))
            (first (shuffle mouth-type)))))
