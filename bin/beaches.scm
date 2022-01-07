;;;
;;;  Copyright (C) 2002-2022  The FreeCol Team
;;;
;;;  This file is part of FreeCol.
;;;
;;;  FreeCol is free software: you can redistribute it and/or modify
;;;  it under the terms of the GNU General Public License as published by
;;;  the Free Software Foundation, either version 2 of the License, or
;;;  (at your option) any later version.
;;;
;;;  FreeCol is distributed in the hope that it will be useful,
;;;  but WITHOUT ANY WARRANTY; without even the implied warranty of
;;;  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
;;;  GNU General Public License for more details.
;;;
;;;  You should have received a copy of the GNU General Public License
;;;  along with FreeCol.  If not, see <http://www.gnu.org/licenses/>.
;;;

(define edge-width 24)
(define edge-height 12)

(define directions
  '((north . 1)
    (north-east . 2)
    (east . 4)
    (south-east . 8)
    (south . 16)
    (south-west . 32)
    (west . 64)
    (north-west . 128)))

(define corners
  '((north 64 . 0)
    (east 128 . 32)
    (south 64 . 64)
    (west 0 . 32)))

(define control-points
  '((north 64 . 12)
    (east 104 . 32)
    (south 64 . 52)
    (west 24 . 32)))

(define small-points
  '((north-west 36 . 26)
    (north 56 . 16)
    (north-east 72 . 16)
    (east 92 . 26)
    (south-east 92 . 38)
    (south 72 . 48)
    (south-west 56 . 48)
    (west 36 . 38)))

(define external-points
  '((north-west 0 . 20)
    (north 40 . 0)
    (north-east 88 . 0)
    (east 128 . 20)
    (south-east 128 . 44)
    (south 88 . 64)
    (south-west 40 . 64)
    (west 0 . 44)))

(define intersections
  '((north-west 12 . 26)
    (north 52 . 6)
    (north-east 78 . 6)
    (east 116 . 26)
    (south-east 116 . 38)
    (south 78 . 58)
    (south-west 52 . 58)
    (west 12 . 38)))

(define decode-style
  (lambda (style)
    (let loop ((remaining-directions (reverse directions))
               (next-style style)
               (result '()))
      (if (or (= 0 next-style)
              (null? remaining-directions))
          result
          (let ((new-style (- next-style (cdr (car remaining-directions)))))
            (loop (cdr remaining-directions)
                  (if (>= new-style 0) new-style next-style)
                  (if (>= new-style 0)
                      (cons (car (car remaining-directions)) result)
                      result)))))))

(define has-edge?
  (lambda (style edge)
    (memq edge style)))


(define get-x
  (lambda (points direction)
    (car (cdr (assq direction points)))))

(define get-y
  (lambda (points direction)
    (cdr (cdr (assq direction points)))))

(define script-fu-make-beaches
  (lambda (img drawable)
    (let* ((width (car (gimp-image-width img)))
           (height (car (gimp-image-height img)))
           (half-width (/ width 2))
           (half-height (/ height 2)))
      (let loop ((count 1))
        (if (< count 256)
            (let* ((image (car (gimp-image-duplicate img)))
                   (pic-layer (car (gimp-image-get-active-drawable image)))
                   (style (decode-style count))
                   (vec (car (gimp-vectors-new image "points"))))

              (gimp-image-undo-disable image)
              (gimp-image-undo-group-start image)
              (gimp-selection-none image)
              
              (gimp-image-add-vectors image vec -1)

              (let* ((points (if (has-edge? style 'north-west)
                                 small-points
                                 external-points))
                     (stroke-id (car (gimp-vectors-bezier-stroke-new-moveto
                                      vec
                                      (get-x points 'north-west)
                                      (get-y points 'north-west)))))

                (let edge-loop ((edges '(north-west north-east south-east south-west))
                                (corners '(north east south west)))
                  (if (null? edges)
                      #t
                      (let ((current-edge (car edges))
                            (next-edge (if (null? (cdr edges))
                                           'north-west
                                           (car (cdr edges))))
                            (current-corner (car corners)))
                        (if (has-edge? style current-edge)
                            (if (has-edge? style next-edge)
                                ;; internal corner
                                (begin
                                  (gimp-vectors-bezier-stroke-lineto
                                   vec stroke-id
                                   (get-x small-points current-corner)
                                   (get-y small-points current-corner))
                                  (gimp-vectors-bezier-stroke-conicto
                                   vec stroke-id
                                   (get-x control-points current-corner)
                                   (get-y control-points current-corner)
                                   (get-x small-points next-edge)
                                   (get-y small-points next-edge)))
                                ;; straight line to external edge
                                (gimp-vectors-bezier-stroke-lineto
                                 vec stroke-id
                                 (get-x external-points next-edge)
                                 (get-y external-points next-edge)))
                            (begin
                              ;; move forward
                              (gimp-vectors-bezier-stroke-lineto
                               vec stroke-id
                               (get-x external-points current-corner)
                               (get-y external-points current-corner))
                              (if (not (has-edge? style next-edge))
                                  ;; external corner
                                  (begin
                                    (if (has-edge? style current-corner)
                                        (begin
                                          (gimp-vectors-bezier-stroke-lineto
                                           vec stroke-id
                                           (get-x intersections current-corner)
                                           (get-y intersections current-corner))
                                          (gimp-vectors-bezier-stroke-conicto
                                           vec stroke-id
                                           (get-x control-points current-corner)
                                           (get-y control-points current-corner)
                                           (get-x intersections next-edge)
                                           (get-y intersections next-edge))))
                                    (gimp-vectors-bezier-stroke-lineto
                                     vec stroke-id
                                     (get-x external-points next-edge)
                                     (get-y external-points next-edge))))))
                        (edge-loop (cdr edges)
                                   (cdr corners)))))

                (gimp-vectors-to-selection
                 vec
                 CHANNEL-OP-ADD
                 TRUE FALSE 0 0)
                (gimp-edit-clear pic-layer)
                (file-png-save-defaults
                 1 image pic-layer
                 (string-append "beach" (number->string count) ".png") "")
                (loop (+ count 1))

                )))))))



(script-fu-register "script-fu-make-beaches"
                    _"Make beaches"
                    _"Make beaches"
                    "Michael Burschik <Michael.Burschik@gmx.de>"
                    "Michael Burschik"
                    "2009-08-09"
                    "RGB GRAY"
                    SF-IMAGE      "Image"            0
                    SF-DRAWABLE   "Drawable"         0)

(script-fu-menu-register "script-fu-make-beaches"
                         "<Image>/Filters")
