(ns dactyl-keyboard.layout
  (:refer-clojure :exclude [use import])
  (:require [scad-clj.scad :refer :all]
            [scad-clj.model :refer :all]
            [dactyl-keyboard.util :refer :all]
            [dactyl-keyboard.switch-hole :refer :all]
            [dactyl-keyboard.layout-helpers :refer :all]
            [dactyl-keyboard.keycaps :refer [sa-cap]]
            [dactyl-keyboard.placement :refer [key-place thumb-place]]
            [unicode-math.core :refer :all]))

(def layouts
  {:y-and-b
; we split the keyholes into pieces for smaller print volume and quicker printing
   {:columns-pieces [(range -1 0) (range 0 2) (range 2 4) (range 4 6)]
    :rows (range 0 5)
                                        ; some of these keys are not
    ; within columns and rows. this is because the case bottom uses a
    ; larger range
    :finger-knockouts [[0 4] [-1 4] [-2 4] [0 5] [-1 5] [-2 5]]
    :around-edge [#_[:sw :k -1 3] [:w :k -1 2] [:w :k -1 1] [:nw :k -1 0]
                  [:n :k 0 0] [:n :k 1 0] [:n :k 2 0] [:n :k 3 0] [:n :k 4 0] [:ne :k 5 0]
                  [:e :k 5 1] [:e :k 5 2] [:e :k 5 3] [:se :k 5 4]
                  [:s :k 4 4] [:s :k 3 4] [:s :k 2 4] [:sw :k 1 4]
                  [:se :t 0 -1] [:s :t 1 -1] [:sw :t 2 -1]
                  [:w :t 2 0] [:nw :t 2 1] [:n :t 1 1]]
    :sides-partitions [[[:n-of-k 4 0] [:ne-of-k 5 0] [:se-of-k 5 4] [:s-of-k 4 4]]
                       [[:at-k 4 1] [:n-of-k 4 0] [:nw-of-k -1 0] [:w-of-k -1 1]]
                       [[:w-of-k -1 1] [:w-of-t 2 0] [:at-t 0 0] [:at-k 0 1]]
                       [[:at-t 1 0] [:w-of-t 2 0] [:sw-of-t 2 -1] [:s-of-t 1 -1]]
                       [[:at-t 1 0] [:s-of-t 2 0] [:s-of-k 4 1] [:at-k 4 1]]]
    :sides-slice-joints [[:s :k 4 :last] [:n :k 4 :first] [:w :k :first 1]
                         [:w :t 2 0] [:s :t 1 -1]]
    :sides-frame-joints  [[[:e :k 5 0 1] [:e :k 5 1 2] [:e :k 5 3 4]]
                          [[:n :k 2 3 0] [:n :k 0 1 0] [:w :k -1 0 1]]
                          [[:w :k -1 1 2] [:w :t 2 -1 0]]
                          [[:w :t 2 0 1] [:s :t 2 1 1]]
                          [[:s :t 1 0 1] [:s :k 2 3 4]]]
    :thumb-glue-joint-left-of [[-1 3] [1 4]]
    :silo-widenings y-and-b-key-silo-widenings}

   :mini
   {:columns-pieces [(range -1 2)]
    :rows (range 2 5)
    :finger-knockouts [[0 4] [-1 4]]
    :around-edge [#_[:sw :k -1 3] [:nw :k -1 2]
                  [:n :k 0 2] [:ne :k 1 2]
                  [:e :k 1 3] [:se :k 1 4]
                  [:se-in :k 0 4] [:se :t 0 -1]
                  [:s :t 1 -1] [:sw :t 2 -1]
                  [:w :t 2 0] [:nw :t 2 1]
                  [:n :t 1 1]]
    :thumb-glue-joint-left-of [[-1 3] [1 4]]
    :silo-widenings (fn [cs rs c r]
                      (cond
                        (= c (first cs)) [0 0.25]
                        (= c (last cs)) [0.25 0]
                        :else [0 0]))}})


(def chosen-layout (layouts :y-and-b))


(def columns-pieces (chosen-layout :columns-pieces))
(def columns (apply concat columns-pieces))
(def rows (chosen-layout :rows))
(def sides-partitions (chosen-layout :sides-partitions))
(def sides-slice-joints (chosen-layout :sides-slice-joints))
(def sides-frame-joints (chosen-layout :sides-frame-joints))
(defn key-silo-widenings [c r]
  ((chosen-layout :silo-widenings) columns rows c r))

                                        ; this defines the keys
                                        ; missing from the finger part
                                        ; that make room for the thumb
(defn finger-has-key-place-p [row column]
  (every? false? (for [[c r] (chosen-layout :finger-knockouts)]
                   (and (= c column) (= r row)))))

                                        ; coordinates of keys around
                                        ; the edge of the finger
                                        ;               >*****v
                                        ;               *.....*
                                        ;               *.....*
                                        ; start here -> ^**...*
                                        ;               xx****<
(def around-edge (chosen-layout :around-edge))


(defn rot1 [s] (concat (rest s) (list (first s))))
(def around-edge-rot1 (rot1 around-edge))

                                        ; the around-edge vector is a
                                        ; list of the key places
                                        ; around the edge in
                                        ; order. this is a description
                                        ; of whether a given key place
                                        ; is around the edge.
(defn around-edge-p [row column]
  (and
   (finger-has-key-place-p row column)
   (or (= column (first columns))
       (= column (last columns))
       (= row (first rows))
       (= row (last rows))
       ; this may not be general to any possible set of knockouts
       (not (finger-has-key-place-p row (inc column)))
       (not (finger-has-key-place-p (inc row) column))
       (not (finger-has-key-place-p (inc row) (dec column))))))


(defn thumb-glue-joint-left-of-p [row column]
  (some true? (for [[c r] (chosen-layout :thumb-glue-joint-left-of)]
                (and (= c column) (= row r)))))


(defn key-shapes-for-columns [shape columns]
  (apply union
         (for [column columns
               row rows
               :when (finger-has-key-place-p row column)]
           (->> shape
                (key-place column row)))))

(def key-holes-pieces (map #(key-shapes-for-columns chosen-single-plate %) columns-pieces))
(def key-holes   (key-shapes-for-columns chosen-single-plate columns))

(def key-blanks-pieces (map #(key-shapes-for-columns chosen-blank-single-plate %) columns-pieces))
(def key-blanks (key-shapes-for-columns chosen-blank-single-plate columns))


(def caps
  (apply union
         (for [column columns
               row rows
               :when (finger-has-key-place-p row column)]
           (->> (sa-cap (if (= column 5) 1 1))
                (key-place column row)))))

(defn thumb-2x-column [shape]
  (thumb-place 0 -1/2 shape))

(defn thumb-2x+1-column [shape]
  (thumb-place 1 -1/2 shape))

(defn thumb-1x-column [shape]
  (union (thumb-place 2 -1 shape)
         (thumb-place 2 0 shape)
         (thumb-place 2 1 shape)))

(defn thumb-layout [shape]
  (union
   (thumb-2x-column shape)
   (thumb-2x+1-column shape)
   (thumb-1x-column shape)))

(def thumbcaps
  (union
   (thumb-2x-column (sa-cap 2))
   (thumb-place 1 -1/2 (sa-cap 2))
   #_(thumb-place 1 1 (sa-cap 1))
   (thumb-1x-column (sa-cap 1))))

(defn reify-column [c] (cond (= c :first) (first columns)
                             (= c :last) (last columns)
                             :else c))

(defn reify-row [r] (cond (= r :first) (first rows)
                          (= r :last) (last rows)
                          :else r))

(defn key-place-fn [notation1]
  "Turn a piece of notation like [:k 3 2] into a function that
  places a shape in the center of the indicated place on the
  keyboard."
  (let [[place-kw col row] notation1
        c (reify-column col)
        r (reify-row row)
        place ({:k key-place, :t thumb-place} place-kw)
        there (partial place c r)]
    there))

(defn sides-place-fns [down out notation1]
  "Turn a piece of notation like [:sw :k 3 2] into some functions that
  place a shape below the outside edge of the indicated place on the
  keyboard. Outside is indicated by the direction ('gravity'); down
  and out are how far downward and outward to go. At outside corners
  we return three such placer functions."
  (let [nby (+ (* 1/2 mount-height) out)
        sby (- nby)
        eby (+ (* 1/2 mount-width) out)
        wby (- eby)
        n [0 nby (- down)]
        s [0 sby (- down)]
        e [eby 0 (- down)]
        w [wby 0 (- down)]
        sw [wby sby (- down)]
        se [eby sby (- down)]
        ne [eby nby (- down)]
        nw [wby nby (- down)]
        there (key-place-fn (rest notation1))
        place-vs
        {:nw [w nw n], :n [n], :ne [n ne e], :e [e],
         :se [e se s], :s [s], :sw [s sw w], :w [w],
         :sw-in [sw], :se-in [se], :nw-in [nw], :ne-in [ne]}
        [grav-kw place-kw col row] notation1
        ]
    (map #(fn [shape] (there (translate % shape)))
         (place-vs grav-kw))))
