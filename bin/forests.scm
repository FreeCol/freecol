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

(define half-x 64)
(define half-y 32)
(define half-width 20)
(define half-height 10)

;;; Increment a binary number represented as a list of ones and zeros,
;;; starting with the least significant bit. Return #f if the number
;;; can not be incremented without adding another bit.
(define increment
  (lambda (lst)
    (let loop ((remaining lst)
               (result '()))
      (if (null? remaining)
          #f
          (if (= 0 (car remaining))
              (append (reverse (cons 1 result))
                      (cdr remaining))
              (loop (cdr remaining)
                    (cons 0 result)))))))

;;; Calculate the point on the line defined by the given point and
;;; slope, at x0.
(define calculate-point
  (lambda (point slope x0)
    (let* ((x (car point))
           (y (cadr point))
           (b (- y (* slope x))))
      (list x0 (+ (* slope x0) b)))))


(define script-fu-cut-forests
  (lambda (img drawable)
    (let* ((height (car (gimp-image-height img)))
           (offset (- height 64))
           (north
            (list half-x (+ offset (- half-y half-height))))
           (east
            (list (+ half-x half-width) (+ offset half-y)))
           (south
            (list half-x (+ offset half-y half-height)))
           (west
            (list (- half-x half-width) (+ offset half-y)))
           (north-east
            (list north (calculate-point north -0.5 128)
                  (calculate-point east -0.5 128)))
           (south-east
            (list east (calculate-point east 0.5 128)
                  (calculate-point south 0.5 128)))
           (south-west
            (list south (calculate-point south -0.5 0)
                  (calculate-point west -0.5 0)))
           (north-west
            (list west (calculate-point west 0.5 0)
                  (calculate-point north 0.5 0) north))
           (rectangles
            (list north-east south-east south-west north-west)))
      (let loop ((count '(1 0 0 0)))
        (if count
            (let* ((image (car (gimp-image-duplicate img)))
                   (pic-layer (car (gimp-image-get-active-drawable image)))
                   (vec (car (gimp-vectors-new image "points"))))

              (gimp-image-undo-disable image)
              (gimp-image-undo-group-start image)
              (gimp-selection-none image)

              (gimp-image-add-vectors image vec -1)

              (let branch-loop ((branches count)
                                (rectangles rectangles)
                                (result '()))
                (if (null? branches)
                    (let ((points
                           (apply append (map (lambda (n) (append n n n)) result))))
                      (gimp-vectors-stroke-new-from-points
                       vec 0 (length points) (list->vector points) TRUE))
                    (let ((branch (car branches))
                          (rectangle (car rectangles)))
                      (branch-loop (cdr branches)
                                   (cdr rectangles)
                                   (append result
                                           (if (= 1 branch)
                                               rectangle
                                               (list (car rectangle))))))))

              (let* ((current-name (car (gimp-image-get-filename img)))
                     (name (substring current-name 0 (- (string-length current-name) 4))))
                (gimp-vectors-to-selection
                 vec
                 CHANNEL-OP-ADD
                 TRUE FALSE 0 0)
                (gimp-edit-clear pic-layer)
                (file-png-save-defaults
                 1 image pic-layer
                 (string-append
                  name
                  (apply string-append (map number->string count))
                  ".png") "")
                (loop (increment count)))))))))



(script-fu-register "script-fu-cut-forests"
                    _"Cut forests"
                    _"Cut forests"
                    "Michael Vehrs <Michael.Burschik@gmx.de>"
                    "Michael Vehrs"
                    "2012-10-27"
                    "RGB GRAY"
                    SF-IMAGE      "Image"            0
                    SF-DRAWABLE   "Drawable"         0)

(script-fu-menu-register "script-fu-cut-forests"
                         "<Image>/Filters")
