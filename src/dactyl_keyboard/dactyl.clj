(ns dactyl-keyboard.dactyl
  (:refer-clojure :exclude [use import])
  (:require [scad-clj.scad :refer :all]
            [scad-clj.model :refer :all]
            [dactyl-keyboard.util :refer :all]
            [dactyl-keyboard.switch-hole :refer :all]
            [dactyl-keyboard.keycaps :refer :all]
            [dactyl-keyboard.placement :refer :all]
            [dactyl-keyboard.layout :refer :all]
            [dactyl-keyboard.connectors :refer :all]
            [dactyl-keyboard.sides-connectors :refer :all]
            [dactyl-keyboard.frame-glue-joint :refer :all]
            [dactyl-keyboard.sides :refer :all]
            [dactyl-keyboard.bottom :refer :all]
            [dactyl-keyboard.sides-pieces :refer :all]
            [dactyl-keyboard.screw-hole :refer :all]
            [unicode-math.core :refer :all]
            [dactyl-keyboard.half-circle-connectors :refer :all]
            [dactyl-keyboard.adafruit-usb :refer :all]
            [dactyl-keyboard.rj11 :refer :all]))




(def thumb
  (union
   thumb-connectors
   (thumb-layout (rotate (/ π 2) [0 0 1] chosen-single-plate))
   (thumb-place 0 -1/2 double-plates)
   (thumb-place 1 -1/2 double-plates)))

(def thumb-blanks
  (union
   (thumb-layout (rotate (/ π 2) [0 0 1] chosen-blank-single-plate))
   (thumb-place 0 -1/2 double-plates-blank)
   (thumb-place 1 -1/2 double-plates-blank)))




;;;;;;;;;;;;;;;;;;;;;;;;;
;; Glue Joints for top ;;
;;;;;;;;;;;;;;;;;;;;;;;;;

(def glue-post (->> (cube post-size post-size glue-joint-height)
                   (translate [0 0 (+ (/ glue-joint-height -2)
                                      plate-thickness)])))
(def glue-post-t  (translate [0 (- (/ mount-height 2) post-adj) 0] glue-post))
(def glue-post-b  (translate [0 (+ (/ mount-height -2) post-adj) 0] glue-post))

(defn fingers-to-thumb-glue-joints-for-columns [columns]
  (apply union
         (for [[column row] [ [-1 3] [1 4] ] :when (some (partial = column) columns)]
           (union
            (key-place (- column 1/2) row
                       (color [1 0 0] glue-joint-center-left))
            (color [1 0 1]
                   (hull (key-place column row web-post-tl)
                         (key-place (- column 1/2) row
                                    (translate [(/ glue-joint-wall-thickness 2) 0 0]
                                               glue-post-t))
                         (key-place column row web-post-bl)
                         (key-place (- column 1/2) row
                                    (translate [(/ glue-joint-wall-thickness 2) 0 0]
                                               glue-post-b))))))))

                                        ; thumb-glue-joints doesn't
                                        ; get the same loopy
                                        ; treatment, because the 2x1
                                        ; key is different
(def thumb-to-fingers-glue-joints
  (union

   (key-place 1/2 4 (color [0 1 0] glue-joint-center-right))
   (color [1 0 1] (hull (thumb-place 0 -1/2 (translate [0 (/ mount-height 2) 0] web-post-tr)) (key-place 1/2 4 (translate [(- 0 (* glue-joint-wall-thickness 3/2)) 0 0] glue-post-t))
                        (thumb-place 0 -1/2 (translate [0 (/ mount-height 2) 0] web-post-br)) (key-place 1/2 4 (translate [(- 0 (* glue-joint-wall-thickness 3/2)) 0 0] glue-post-b))))

   (key-place -3/2 3 (color [0 1 0] glue-joint-center-right))
   (color [1 0 1] (hull (thumb-place 2 1 web-post-tr) (key-place -3/2 3 (translate [(- 0 (* 3/2 glue-joint-wall-thickness)) 0 0] glue-post-t))
                        (thumb-place 2 1 web-post-br) (key-place -3/2 3 (translate [(- 0 (* 3/2 glue-joint-wall-thickness)) 0 0] glue-post-b))))
   ;; this below is a block so you can clamp this joint when you glue it.
   (color [0 0 1] (hull (->> glue-post-t
                             (translate [(* -7 glue-joint-wall-thickness) 0 0])
                             (key-place -3/2 3))
                        (->> glue-post-b
                             (translate [(* -7 glue-joint-wall-thickness) 0 0])
                             (key-place -3/2 3))
                        (->> glue-post-t
                             (translate [(* -1 glue-joint-wall-thickness) 0 0])
                             (key-place -3/2 3))
                        (->> glue-post-b
                             (translate [(* -1 glue-joint-wall-thickness) 0 0])
                             (key-place -3/2 3))))))

(def right-glue-joints-for-fingerpieces
  (let [rgj-for-this
        (fn [leftmostp rightmostp columns]
          (if (not rightmostp)
            (let [column (last columns)
                  joint-column (+ column 1/2)
                  other-way (- column 1/2)]
              (apply union
                     (for [row rows :when (not (or (and (= column 0) (= row 4))
                                                   (and (= column -1) (= row 4))))]
                       (union
                                        ; the actual paddle
                        (key-place joint-column row
                                   (color [0 1 0] glue-joint-center-right))
                                        ; connect paddle to key-place
                        (color [1 0 1]
                               (hull
                                (key-place column row web-post-tr)
                                (key-place column row (translate [(- (* cherry-bezel-width 1/2)) 0 0] web-post-tr))
                                (key-place joint-column row
                                           (translate [(- 0 (* glue-joint-wall-thickness 3/2)) 0 0]
                                                      glue-post-t))
                                (key-place joint-column row
                                           (translate [(- 0 (* glue-joint-wall-thickness 2/2)) 0 0]
                                                      glue-post-t))
                                (key-place column row web-post-br)
                                (key-place column row (translate [(- (* cherry-bezel-width 1/2)) 0 0] web-post-br))
                                (key-place joint-column row
                                           (translate [(- 0 (* glue-joint-wall-thickness 2/2)) 0 0]
                                                      glue-post-b))
                                (key-place joint-column row
                                           (translate [(- 0 (* glue-joint-wall-thickness 3/2)) 0 0]
                                                      glue-post-b))))))))))]
    (map #_(fn [a b c] ()) rgj-for-this
         (cons true (repeat false))
         (concat (repeat (- (count columns-pieces) 1) false) '(true))
         columns-pieces)))

(def left-glue-joints-for-fingerpieces
  (let [per-column-web-post-transform
        (fn [column]
                                        ; what these should be depends on the column; see key-place above
          (cond
                                        ; these columns go up relative to the previous
            (and (>= column 3) (< column 5))
            (fn [post] (translate [1.3 0 0] post))
                                        ; this column goes down relative to the previous
            (and (>= column 2) (< column e))
            (fn [post] post)
            :else (fn [post] post)))
        lgj-for-this
        (fn [leftmostp rightmostp columns]
          (if (not leftmostp)
            (let [column (first columns)
                  joint-column (- column 1/2)
                  other-way (+ column 1/2)]
              (apply union
                     (for [row rows :when (not (or (and (= column 1) (= row 4))
                                                   (and (= column 0) (= row 4))))]
                       (union
                                        ; the actual paddle
                        (key-place joint-column row
                                   (color [1 0 0] glue-joint-center-left))
                                        ; connect paddle to key-place
                        (color [1 0 1]
                               (hull
                                (key-place joint-column row glue-post-t)
                                (key-place joint-column row (translate [(* glue-joint-wall-thickness 1/2) 0 0] glue-post-t))
                                (key-place column row web-post-tl)
                                (key-place column row (translate [(* cherry-bezel-width 1/2) 0 0] web-post-tl))
                                (key-place joint-column row glue-post-b)
                                (key-place joint-column row (translate [(* glue-joint-wall-thickness 1/2) 0 0] glue-post-b))
                                (key-place column row web-post-bl)
                                (key-place column row (translate [(* cherry-bezel-width 1/2) 0 0] web-post-bl))))))))))]
    (map #_(fn [a b c] ()) lgj-for-this
         (cons true (repeat false))
         (concat (repeat (- (count columns-pieces) 1) false) '(true))
         columns-pieces)))
;;;;;;;;;;;;;;;;;;
;; Final Export ;;
;;;;;;;;;;;;;;;;;;

(defn dactyl-top-right-plusses [key-pieces]
  ; agh i made bad names and now i pay for it
  (let [pieces-of-pieces (map vector
                              (map fingers-to-thumb-glue-joints-for-columns columns-pieces)
                              right-glue-joints-for-fingerpieces
                              key-pieces
                              connectors-inside-fingerpieces
                              left-glue-joints-for-fingerpieces
                              (for [cols columns-pieces]
                                (let [teensy-column (nth teensy-bracket-at 1)]
                                  (if (and (>= teensy-column (first cols))
                                           (<= teensy-column (last cols)))
                                    (->> screw-hole-for-teensy
                                         ((key-place-fn teensy-bracket-at)))))))]
    (for [pieces-of-this-piece pieces-of-pieces]
      (apply union pieces-of-this-piece))))

(defn dactyl-top-right-minuses [key-pieces]
  (let [pieces-of-pieces (map vector
                              screw-holes-in-fingerpieces)]
    (for [pieces-of-this-piece pieces-of-pieces]
      (apply union pieces-of-this-piece))))

(defn dactyl-top-right-pieces [key-pieces]
  (for [[plus minus] (map vector
                          (dactyl-top-right-plusses key-pieces)
                          (dactyl-top-right-minuses key-pieces))]
        (difference plus minus)))

(def dactyl-top-right-thumb
  (difference
   (union thumb thumb-to-fingers-glue-joints)
   screw-holes-in-thumb))

(def define-sides-with-right-ports
  (define-module "SidesWithRightPorts"
    (with-fn 12
      (difference
       (union sides-right
              usb-nice-plate
              rj11-nice-plate)
       (usb-cutout-place adafruit-usb-cutout)
       (rj11-cutout-place rj11-cutout)))))

(def define-sides-with-left-ports
  (define-module "SidesWithLeftPorts"
    (with-fn 12
      (difference
       (union sides-right
              rj11-nice-plate)
       (rj11-cutout-place rj11-cutout)))))

(defn say-spit [& body]
  (do
    (print (format "%s\n" (first body)))
    (apply spit body)))

(say-spit "things/switch-hole.scad"
      (write-scad chosen-single-plate))

(say-spit "things/dactyl-top-right-thumb.scad"
          (write-scad
           (use "key-place.scad")
           dactyl-top-right-thumb
           (sides-connectors-thumb-from-notation sides-frame-joints)))

(say-spit "things/dactyl-top-left-thumb.scad"
          (write-scad
           (use "key-place.scad")
           (mirror [1 0 0]
                   dactyl-top-right-thumb
                   (sides-connectors-thumb-from-notation sides-frame-joints)))

(doseq [[partno part1 part2]
        (map vector (range)
             (dactyl-top-right-pieces key-holes-pieces)
             (sides-connectors-frame-from-notation sides-frame-joints))]
  (do
    (say-spit (format "things/dactyl-top-right-%02d.scad" partno)
            (write-scad
             (use "key-place.scad")
             (union part1 part2)))
    (say-spit (format "things/dactyl-top-left-%02d.scad" partno)
            (write-scad
             (use "key-place.scad")
             (mirror [1 0 0]
                     (union part1 part2))))))

(say-spit "things/dactyl-top-right-all.scad"
          (write-scad
           (use "key-place.scad")
           (union dactyl-top-right-thumb
                  (apply union (dactyl-top-right-pieces key-holes-pieces))
                  caps
                  thumbcaps)))

(say-spit "things/dactyl-top-left-all.scad"
          (write-scad
           (use "key-place.scad")
           (mirror [1 0 0]
                   (union dactyl-top-right-thumb
                          (apply union (dactyl-top-right-pieces key-holes-pieces))
                          caps
                          thumbcaps))))

(def sides-slices-right
  (pieces-with-x-pins-and-holes-faster (* sides-radius 3/4)
                                the-sides-slice-joints
                                sides-slice-intersects
                                (call-module "SidesWithRightPorts")
                                sides-regions))

(def sides-slices-left
  (pieces-with-x-pins-and-holes-faster (* sides-radius 3/4)
                                the-sides-slice-joints
                                sides-slice-intersects
                                (call-module "SidesWithLeftPorts")
                                sides-regions))

(doseq [[partno part1 part2] (map vector (range)
                                  sides-slices-right
                                  (sides-connectors-sides-from-notation
                                   sides-frame-joints
                                   sides-slices-right))]
  (say-spit (format "things/sides-right-%02d.scad" partno)
            (write-scad
             (use "key-place.scad")
             define-sides-with-right-ports
             (union part1 part2
                    #_(union dactyl-top-right-thumb
                           (apply union (dactyl-top-right-pieces
                                         key-holes-pieces)))))))

(doseq [[partno part1 part2] (map vector (range)
                                  sides-slices-left
                                  (sides-connectors-sides-from-notation
                                   sides-frame-joints
                                   sides-slices-right))]
  (say-spit (format "things/sides-left-%02d.scad" partno)
            (write-scad
             (use "key-place.scad")
             define-sides-with-left-ports
             (mirror [1 0 0] (union part1 part2)))))

(say-spit "things/splits-right.scad"
          (write-scad
           (use "key-place.scad")
           (union
            (union dactyl-top-right-thumb
                   (apply union (dactyl-top-right-pieces key-holes-pieces)))
            sides-slice-intersects
            )))

(say-spit "things/joins-right.scad"
          (write-scad
           (use "key-place.scad")
           define-sides-with-right-ports
           (union
            (union dactyl-top-right-thumb
                   (apply union (dactyl-top-right-pieces key-holes-pieces)))
            (map #(% (rotate (* 1/4 τ) [0 1 0] (cylinder [10 0] 10))) the-sides-slice-joints))))

(say-spit "things/keys.scad"
          (write-scad
           (use "key-place.scad")
           (union
            (union caps thumbcaps))))

(say-spit "things/dactyl-photo.scad"
          (write-scad
           (use "key-place.scad")
           define-sides-with-right-ports
           (union
            sides-right
            bottom-right
            (union caps thumbcaps)
            (union dactyl-top-right-thumb
                   (apply union (dactyl-top-right-pieces key-holes-pieces)))
            )))

(say-spit "things/dactyl-bottom-right.scad"
          (write-scad
           (use "key-place.scad")
           (union
            bottom-right)
            #_(union dactyl-top-right-thumb
                   (apply union
                          (dactyl-top-right-pieces key-holes-pieces))))))

(say-spit "things/dactyl-bottom-left.scad"
          (write-scad
           (use "key-place.scad")
           (mirror [1 0 0]
                   (union
                    bottom-right))))

(def entire-x 280)
(def entire-y 200)
(def entire-z 200)
; set so that there aren't any little bits in the first slice
(def bottom-slice-offset (* mount-width 0.58))
(def bottom-slice-spacing (* mount-width 3.2))
(def bottom-glue-tolerance 0.2)
(doseq [slice (range (/ entire-x bottom-slice-spacing))]
  (doseq [[ab-letter ab-number] [["a" 0] ["b" 1]]]
    (do
      (say-spit (format "things/dactyl-bottom-right-%02d%s.scad"
                        slice ab-letter)
                (write-scad
                 (use "key-place.scad")
                 (use "vertical-prisms.scad")
                 (render (->> (call-module "vertical_prisms_slice"
                                           entire-x entire-y entire-z
                                           bottom-slice-spacing
                                           bottom-glue-tolerance
                                           ab-number
                                           slice)
                              (rotate (* 3/100 τ) [0 0 1])
                              (translate [bottom-slice-offset 0
                                          (* 1/3 entire-z)])
                              (intersection bottom-right)))))
      (say-spit (format "things/dactyl-bottom-left-%02d%s.scad"
                        slice ab-letter)
                (write-scad
                 (use "key-place.scad")
                 (use "vertical-prisms.scad")
                 (mirror [1 0 0]
                         (render (->> (call-module "vertical_prisms_slice"
                                                   entire-x entire-y entire-z
                                                   bottom-slice-spacing
                                                   bottom-glue-tolerance
                                                   ab-number
                                                   slice)
                                      (rotate (* 3/100 τ) [0 0 1])
                                      (translate [bottom-slice-offset 0
                                                  (* 1/3 entire-z)])
                                      (intersection bottom-right)))))))))

(say-spit "things/screw-hole-top.scad"
          (write-scad
           (use "vertical-prisms.scad")
           (screw-hole-pillar-upper screw-hole-pillar-height)))
