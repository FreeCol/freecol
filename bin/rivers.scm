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

;;; On a Unix-like operating system, change to your main freecol
;;; directory, and call this gimp script like this:
;;;
;;; gimp --no-data --no-fonts --no-interface -b - < rivers.scm

;;; the freecol image directory
(define images "data/rules/classic/resources/images/")

;;; Increment a base-n number represented as a list of digits,
;;; starting with the least significant digit. Return #f if the number
;;; can not be incremented without adding another digit.
(define increment
  (lambda (base lst)
    (let ((max-digit (- base 1)))
      (let loop ((remaining lst)
                 (result '()))
        (if (null? remaining)
            #f
            (if (< (car remaining) max-digit)
                (append (reverse (cons (+ 1 (car remaining)) result))
                        (cdr remaining))
                (loop (cdr remaining)
                      (cons 0 result))))))))

(let* ((north-east
        #(#f
          #((94 15) (98 17) (66 32) (62 32))
          #((92 14) (100 18) (67 32) (61 32))))
       (south-east
        #(#f
          #((98 47) (94 49) (64 34) (64 30))
          #((100 46) (92 50) (64 35) (64 29))))
       (south-west
        #(#f
          #((34 49) (30 47) (62 32) (66 32))
          #((36 50) (28 46) (61 32) (67 32))))
       (north-west
        #(#f
          #((30 17) (34 15) (64 30) (64 34))
          #((28 18) (36 14) (64 29) (64 35))))
       (centre '(64 32))
       (points
        (list north-east south-east south-west north-west))
       (ocean (car (file-png-load 1 (string-append images "terrain/ocean/center0.png") ""))))

  (let loop ((xcount '(1 0 0 0)))
    (if xcount
        (let* ((image (car (gimp-image-duplicate ocean)))
               (pic-layer (car (gimp-image-get-active-drawable image)))
               (vec (car (gimp-vectors-new image "points"))))

          (gimp-image-undo-disable image)
          (gimp-image-undo-group-start image)
          (gimp-selection-none image)
          (gimp-image-add-vectors image vec -1)

          (let branch-loop ((count xcount)
                            (look-ahead (append xcount xcount))
                            (points (append points points))
                            (result '()))
            (if (null? count)
                (let ((points (apply append result)))
                  (gimp-vectors-stroke-new-from-points
                   vec 0 (length points) (list->vector points) FALSE)
                  (gimp-vectors-to-selection
                   vec
                   CHANNEL-OP-ADD
                   TRUE FALSE 0 0)
                  (gimp-selection-invert image)
                  (gimp-edit-clear pic-layer)
                  (file-png-save-defaults
                   1 image pic-layer
                   (string-append
                    images
                    "river/river"
                    (apply string-append (map number->string xcount))
                    ".png") "")
                  (loop (increment 3 xcount)))
                (let* ((size (car count))
                       (coordinates (vector-ref (car points) size)))
                  (if (= 0 size)
                      (branch-loop (cdr count)
                                   (cdr look-ahead)
                                   (cdr points)
                                   result)
                      (let ((next-branch
                             (let loop ((branches (cdr look-ahead))
                                        (index 1))
                               (if (or (null? branches)
                                       (= 4 index))
                                   #f
                                   (let ((next-size (car branches)))
                                     (if (= 0 next-size)
                                         (loop (cdr branches)
                                               (+ index 1))
                                         (cons index next-size)))))))
                        (if next-branch
                            (let* ((index (car next-branch))
                                   (next-size (cdr next-branch))
                                   (next-coordinates
                                    (vector-ref (list-ref points index) next-size))
                                   (p (vector-ref coordinates 1))
                                   (a (vector-ref next-coordinates 0)))
                              (branch-loop (cdr count)
                                           (cdr look-ahead)
                                           (cdr points)
                                           (append result
                                                   (case index
                                                     ((1) ;; quarter turn
                                                      (let ((c (vector-ref coordinates 2)))
                                                        (list p p p c a c a a a)))
                                                     ((2) ;; straight line
                                                      (list p p p a a a))
                                                     ((3) ;; three-quarter turn
                                                      (let ((c (vector-ref coordinates 3)))
                                                        (list p p p c a c a a a)))))))
                            ;; single branch
                            (let ((p (vector-ref coordinates 0))
                                  (a (vector-ref coordinates 1))
                                  (c centre))
                              (branch-loop
                               '() '() '()
                               (list p p p c c c a a a)))))))))))))

(gimp-quit 0)
