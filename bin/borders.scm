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
;;; gimp --no-data --no-fonts --no-interface -b - < borders.scm

(let* ((N '(64 0))
       (NE '((0 0) (128 0) (128 64)))
       (E '(128 32))
       (SE '((128 0) (128 64) (0 64)))
       (S '(64 64))
       (SW '((128 64) (0 64) (0 0)))
       (W '(0 32))
       (NW '((0 64) (0 0) (128 0)))
       (directions (list N NE E SE S SW W NW))
       (names (map symbol->string '(N NE E SE S SW W NW)))
       (radius 10)
       ;; the freecol image directory
       (directory "data/rules/classic/resources/images/terrain/"))

  (let terrain-loop ((terrains '("arctic" "desert" "grassland" "highSeas"
                                 "marsh" "ocean" "plains" "prairie"
                                 "savannah" "swamp" "tundra" "unexplored")))
    (if (null? terrains)
        (gimp-quit 0)
        (let centre-loop ((centres '("0" "1"))
                          (borders '("_even" "_odd")))
          (if (null? centres)
              (terrain-loop (cdr terrains))
              (let* ((tile (car (file-png-load 1 (string-append directory (car terrains)
                                                                "/center" (car centres) ".png") "")))
                     (drawable (car (gimp-image-get-active-drawable tile))))

                (if (= (car (gimp-drawable-is-rgb drawable)) 0)
                    (gimp-image-convert-rgb tile))

                (let direction-loop ((directions directions)
                                     (names names))
                  (if (null? directions)
                      (centre-loop (cdr centres)
                                   (cdr borders))
                      (let* ((image (car (gimp-image-duplicate tile)))
                             (pic-layer (car (gimp-image-get-active-drawable image)))
                             (direction (car directions)))

                        (gimp-image-undo-disable image)
                        (gimp-image-undo-group-start image)
                        (gimp-selection-none image)

                        (if (number? (car direction))
                            (begin
                              (gimp-ellipse-select
                               image
                               (- (list-ref direction 0) radius)
                               (- (list-ref direction 1) radius)
                               (* 2 radius)
                               (* 2 radius)
                               CHANNEL-OP-ADD TRUE TRUE 10)
                              (gimp-selection-invert image))
                            ;; first, create an unfeathered triangular
                            ;; selection, so that the clear will
                            ;; properly pick up areas near the image
                            ;; borders
                            (let ((points (apply append direction)))
                              (gimp-free-select
                               image
                               (length points)
                               (list->vector points)
                               CHANNEL-OP-ADD
                               TRUE FALSE 0)
                              (gimp-selection-invert image)
                              (gimp-edit-clear pic-layer)
                              ;; now, grow and feather the selection for a gradient effect
                              (gimp-selection-grow image 20)
                              (gimp-selection-feather image 10)))

                        (gimp-edit-clear pic-layer)

                        (file-png-save-defaults
                         1 image pic-layer
                         (string-append directory (car terrains)
                                        "/border_" (car names)
                                        (car borders) ".png")
                         "")
                        (direction-loop (cdr directions)
                                        (cdr names)))))))))))