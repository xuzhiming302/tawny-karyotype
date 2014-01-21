;; The contents of this file are subject to the LGPL License, Version 3.0.

;; Copyright (C) 2012, Newcastle University

;; This program is free software: you can redistribute it and/or modify
;; it under the terms of the GNU General Public License as published by
;; the Free Software Foundation, either version 3 of the License, or
;; (at your option) any later version.

;; This program is distributed in the hope that it will be useful,
;; but WITHOUT ANY WARRANTY; without even the implied warranty of
;; MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
;; GNU General Public License for more details.

;; You should have received a copy of the GNU General Public License
;; along with this program.  If not, see http://www.gnu.org/licenses/.

(ns ^{:doc "Defining event information for human karyotypes."
      :author "Jennifer Warrender"}
ncl.karyotype.events
  (:use [tawny.owl])
  (:require [ncl.karyotype [karyotype :as k]]
            [ncl.karyotype [human :as h]]))

(defontology events
  :iri "http://ncl.ac.uk/karyotype/events"
  :prefix "evn:"
  :comment "Event ontology for Human Karyotype Ontology, written using
  the tawny-owl library.")

(defclass Event)

;; define object properties
;; event object properties
(as-inverse
 (defoproperty hasEvent
   :range Event
   :domain k/Karyotype)
 (defoproperty isEventOf))

(as-inverse
 (defoproperty hasDirectEvent
   :subproperty hasEvent)
 (defoproperty isDirectEventOf
   :subproperty isEventOf))

;; due to build dependancy, the subproperty chain axiom will be added
;; in named-clj
(as-inverse
 (defoproperty hasDerivedEvent
   :subproperty hasEvent)
 (defoproperty isDerivedEventOf
   :subproperty isEventOf))

;; breakpoint object properties
(as-inverse
 (defoproperty hasBreakPoint
   :range k/ChromosomeComponent
   :domain k/Karyotype)
 (defoproperty isBreakPointOf))

(as-inverse
 (defoproperty hasReceivingBreakPoint
   :subproperty hasBreakPoint)
 (defoproperty isReceivingBreakPointOf
   :subproperty isBreakPointOf))

(as-inverse
 (defoproperty hasProvidingBreakPoint
   :subproperty hasBreakPoint)
 (defoproperty isProvidingBreakPointOf
   :subproperty isBreakPointOf))

;; AUXILLARY FUNCTIONS
(defn- parentband? [band]
  "Determine if the given band is the parent band"
  (re-find #"HumanChromosomeBand" band))

;; TODO Update methodology st making use of the ontology instead of
;; string manipulation.
(defn get-telomere [band]
  "Returns associated telomere of given band."
    (let [s1 (second (clojure.string/split (str band) #"human#"))
          s2 (first (clojure.string/split s1 #"Band|Telomere|Centromere"))
          s3 (first (clojure.string/split s1 #"Chromosome"))]
      (owl-class
       ncl.karyotype.human/human
       (cond
        (h/str-pband? s1)
        (str s2 "BandpTer")
        (h/str-qband? s1)
        (str s2 "BandqTer")
        (re-find #"[\d]+" s1)
        (str s2 "Telomere")
        (parentband? s1)
        (str s3 "Telomere")
        (re-find #"HumanTelomere|HumanCentromere" s1)
        (str s2 "Telomere")
        :default
        (throw (IllegalArgumentException.
                (str "Band not recognized:" band)))))))

;; hasEvent auxiliary functions
(defn some-event [axiom]
  "Returns a LazySeq of SomeValuesFrom hasEvent restrictions."
  (owl-some hasEvent axiom))

(defn exactly-event [n axiom]
  {:pre (number? n)}
  "Returns a (single) ExactCardinality hasEvent restriction."
  (exactly n hasEvent axiom))

(defn event [n axiom]
  "(Either) Returns a LazySeq SomeValuesFrom or one ExactCardinality
hasEvent restrictions."
  (if (nil? n)
    (some-event axiom)
    (exactly-event n axiom)))

;; hasDirectEvent auxiliary functions
(defn some-direct-event [axiom]
  "Returns a LazySeq of SomeValuesFrom hasDirectEvent restrictions"
  (owl-some hasDirectEvent axiom))

(defn exactly-direct-event [n axiom]
  {:pre (number? n)}
  "Returns a (single) ExactCardinality hasDirectEvent restriction."
  (exactly n hasDirectEvent axiom))

(defn direct-event [n axiom]
  "(Either) Returns a LazySeq SomeValuesFrom or one ExactCardinality
hasDirectEvent restrictions."
  (if (nil? n)
    (some-direct-event axiom)
    (exactly-direct-event n axiom)))

;; OWL CLASSES - EVENTS
(as-disjoint-subclasses
 Event
 (defclass Addition)
 (defclass Deletion)
 (defclass Duplication)
 (defclass Fission)
 (defclass Insertion)
 (defclass Inversion)
 (defclass Quadruplication)
 (defclass Translocation)
 (defclass Triplication))

(as-disjoint-subclasses
 Duplication
 (defclass DirectDuplication)
 (defclass InverseDuplication))

(as-disjoint-subclasses
 Insertion
 (defclass DirectInsertion)
 (defclass InverseInsertion))

(as-disjoint-subclasses
 Triplication
 (defclass DirectTriplication)
 (defclass InverseTriplication))


;; FUNCTIONS
;; TODO - Precondition for each function?

;; Addition patterns
(defn addition-chromosome [chromosome]
  {:pre (true? (h/chromosome? chromosome))}
  "Pattern - returns part of chromosomal addition axiom."
  (owl-and Addition chromosome))

(defn addition-band [band]
  {:pre (true? (h/band? band))}
  "Pattern - returns part of chromosomal band addition axiom."
  (owl-and Addition
           (owl-some hasBreakPoint band)))

;; Chromosomal Addition OR Chromosomal Band Addition
;; Invovles only 1 chromosome
(defn addition [n chrom_band]
  "Returns an addition retriction. N is the number of addition
restrictions. CHROM_BAND is either of type HumanChromosome or
HumanChromosomeBand."
  ;; In order for superclass? to work, need to use the human ontology.
  (with-ontology
    ncl.karyotype.human/human
    (cond
     ;; If chrom_band is of type HumanChromosome then restriction
     ;; represents a chromosomal gain.
     (or
      (= h/HumanChromosome chrom_band)
      (superclass? chrom_band h/HumanChromosome))
     (direct-event n (addition-chromosome chrom_band))
     ;; If chrom_band is of type HumanChromosomeBand then
     ;; restriction represents a chromosomal band addition.
     (or
      (= h/HumanChromosomeBand chrom_band)
      (superclass? chrom_band h/HumanChromosomeBand))
     (direct-event n (addition-band chrom_band))
     :default
     (throw
      (IllegalArgumentException.
       (str "Addition expects a Chromosome or ChromosomeBand. Got:"
            chrom_band))))))

;; Deletion patterns
(defn deletion-chromosome [chromosome]
  {:pre (true? (h/chromosome? chromosome))}
  "Pattern - returns chromosomal deletion axiom."
  (owl-and Deletion chromosome))

(defn deletion-band [band1 band2]
  {:pre [(true? (h/band? band1)) (true? (h/band? band2))]}
  "Pattern - return chromosomal band deletion axiom."
  (owl-and Deletion
           (owl-some hasBreakPoint band1 band2)))

;; Chromosomal Deletion OR Chromosomal Band Deletion : includes
;; Terminal deletion with a break AND Interstitial deletion with
;; breakage and reuinion (::) of bands.
;; Invovles only 1 chromosome
(defn deletion
  "Returns a deletion retriction.
n is the number of deletion restrictions.
chrom_band is either of type HumanChromosome or HumanChromosomeBand.
band, band1, band2 are of type HumanChromosomeBand."
  ([n chrom_band]
     ;; In order for superclass? to work, need to use the human ontology.
     (with-ontology
       ncl.karyotype.human/human
       (cond
        ;; If chrom_band is of type HumanChromosome then restriction
        ;; represents a chromosomal loss.
        (or
         (= h/HumanChromosome chrom_band)
         (superclass? chrom_band h/HumanChromosome))
        (direct-event n (deletion-chromosome chrom_band))
        ;; If chrom_band is of type HumanChromosomeBand then
        ;; restriction represents a terminal band deletion with a break
        ;; (:).
        (or
         (= h/HumanChromosomeBand chrom_band)
         (superclass? chrom_band h/HumanChromosomeBand))
        (direct-event n (deletion-band chrom_band (get-telomere chrom_band)))
        :default
        (throw
         (IllegalArgumentException.
          (str "Deletion expects a HumanChromosome or
               HumanChromosomeBand. Got:" chrom_band))))))
  ([n band1 band2]
     ;; This represents Interstitial band deletion with breakage and
     ;; reunion (::).  band1, band2 are of type HumanChromosomeBand.
     (direct-event n (deletion-band band1 band2))))

;; TODO DO I need to differentiate between terminal and interstitial?
(as-disjoint-subclasses
 Deletion
 (defclass DeletionTerminal)
 (defclass DeletionInterstitial))

(defn terminal-pattern [n band]
  (exactly n hasDirectEvent
           (owl-and DeletionTerminal
                    (owl-some hasBreakPoint band
                              (get-telomere band)))))

(defn interstitial-pattern [n band1 band2]
  (exactly n hasDirectEvent
           (owl-and DeletionInterstitial
                    (owl-some hasBreakPoint band1 band2))))

(defn deletion-new
  "Returns a deletion retriction.
n is the number of deletion restrictions.
chrom_band is either of type HumanChromosome or HumanChromosomeBand.
band, band1, band2 are of type HumanChromosomeBand."
  ([n chrom_band]
     ;; In order for superclass? to work, need to use the human ontology.
     (with-ontology
       ncl.karyotype.human/human
       (cond
        ;; If chrom_band is of type HumanChromosome then restriction
        ;; represents a chromosomal loss.
        (or
         (= h/HumanChromosome chrom_band)
         (superclass? chrom_band h/HumanChromosome))
        (deletion-chromosome n chrom_band)
        ;; If chrom_band is of type HumanChromosomeBand then
        ;; restriction represents a terminal band deletion with a break
        ;; (:).
        (or
         (= h/HumanChromosomeBand chrom_band)
         (superclass? chrom_band h/HumanChromosomeBand))
        (terminal-pattern n chrom_band)
        :default
        (throw
         (IllegalArgumentException.
          (str "Deletion expects a HumanChromosome or
               HumanChromosomeBand. Got:" chrom_band))))))
  ([n band1 band2]
     ;; This represents Interstitial band deletion with breakage and
     ;; reunion (::).  band1, band2 are of type HumanChromosomeBand.
     (interstitial-pattern n band1 band2)))

;; TODO Necessary?
;; (defn chromosome-event [event chromosome]
;;   (owl-and event chromosome))
;; (defn band-event [event & bands]
;;   (owl-and event (owl-some hasBreakPoint bands)))

;; Chromosomal Band Duplication
;; Can be preceeded by the triplets dir or inv to indicate direct or
;; inverted direction
;; Invovles only 1 chromosome
(defn duplication [n band1 band2]
  "Returns a duplication retriction.
n is the number of duplication restrictions.
band1, band2 are of type HumanChromosomeBand."
  (direct-event n (owl-and Duplication
                           (owl-some hasBreakPoint band1 band2))))

;; Chromosomal Band DirectDuplication
;; Invovles only 1 chromosome
(defn direct-duplication [n band1 band2]
  "Returns a direct-duplication retriction.
n is the number of direct-duplication restrictions.
band1, band2 are of type HumanChromosomeBand."
  (direct-event n (owl-and DirectDuplication
                           (owl-some hasBreakPoint band1 band2))))

;; Chromosomal Band InverseDuplication
;; Invovles only 1 chromosome
(defn inverse-duplication [n band1 band2]
  "Returns an inverse-duplication retriction.
n is the number of inverse-duplication restrictions.
band1, band2 are of type HumanChromosomeBand."
  (exactly n hasDirectEvent
           (owl-and InverseDuplication
                   (owl-some hasBreakPoint band1 band2))))

;; Chromosomal Band Fission AKA Centric fission - break in the centromere
;; Involves only 1 chromosome
(defn fission [n band]
  "Returns a fission retriction.
n is the number of fission restrictions.
band is of type HumanChromosomeBand."
  (exactly n hasDirectEvent
           (owl-and Fission
                   (owl-some hasBreakPoint band
                            (get-telomere band)))))

;; Chromosomal Band Insertion
;; Can be preceeded by the triplets dir or inv to indicate direct or
;; inverted direction
;; Involves at most 2 chromosomes
(defn insertion [n band1 band2 band3]
  "Returns an insertion retriction.
n is the number of insertion restrictions.
band1, band2, band3 is of type HumanChromosomeBand."
  (exactly n hasDirectEvent
           (owl-and Insertion
                   (owl-some hasReceivingBreakPoint band1)
                   (owl-some hasProvidingBreakPoint band2 band3))))

;; Choromosomal Band DirectInsertion
;; Involves at most 2 chromosomes
(defn direct-insertion [n band1 band2 band3]
  "Returns a direct-insertion retriction.
n is the number of direct-insertion restrictions.
band1, band2, band3 is of type HumanChromosomeBand."
  (exactly n hasDirectEvent
           (owl-and DirectInsertion
                   (owl-some hasReceivingBreakPoint band1)
                   (owl-some hasProvidingBreakPoint band2 band3))))

(as-disjoint-subclasses
 DirectInsertion
 (defclass DirectInsertionOneChromosome)
 (defclass DirectInsertionTwoChromosome))

;; Make DirectInsertion equivalent to DirectInsertionOneChromosome
;; and DirectInsertionTwoChromosome?
(defn direct-insertion-new
  "Returns a direct-insertion retriction.
n is the number of direct-insertion restrictions.
band1, band2, band3 is of type HumanChromosomeBand."
  ([n chrom1] {:pre (= 3 (count chrom1))}
     (exactly n hasDirectEvent
              (owl-and DirectInsertionOneChromosome
                      (owl-some hasReceivingBreakPoint (first chrom1))
                      (owl-some hasProvidingBreakPoint
                               (second chrom1)
                               (second (rest chrom1))))))
  ([n chrom1 chrom2] {:pre [(and (= 1 (count chrom1)) (= 2 (count chrom2)))]}
     (exactly n hasDirectEvent
              (owl-and DirectInsertionTwoChromosome
                      (owl-some hasReceivingBreakPoint (first chrom1))
                      (owl-some hasProvidingBreakPoint
                               (first chrom2)
                               (second chrom2))))))

;; Choromosomal Band InverseInsertion
;; Involves at most 2 chromosomes
(defn inverse-insertion [n band1 band2 band3]
  "Returns an inverse-insertion retriction.
n is the number of inverse-insertion restrictions.
band1, band2, band3 is of type HumanChromosomeBand."
  (exactly n hasDirectEvent
           (owl-and InverseInsertion
                   (owl-some hasReceivingBreakPoint band1)
                   (owl-some hasProvidingBreakPoint band2 band3))))

(as-disjoint-subclasses
 InverseInsertion
 (defclass InverseInsertionOneChromosome)
 (defclass InverseInsertionTwoChromosome))

;; Make InverseInsertion equivalent to InverseInsertionOneChromosome
;; and InverseInsertionTwoChromosome?
(defn inverse-insertion-new
  "Returns a inverse-insertion retriction.
n is the number of direct-insertion restrictions.
chrom1, chrom2 are vectors that contain HumanChromosomeBand."
  ([n chrom1] {:pre (= 3 (count chrom1))}
     (exactly n hasDirectEvent
              (owl-and InverseInsertionOneChromosome
                      (owl-some hasReceivingBreakPoint (first chrom1))
                      (owl-some hasProvidingBreakPoint
                               (second chrom1)
                               (second (rest chrom1))))))
  ([n chrom1 chrom2] {:pre [(and (= 1 (count chrom1)) (= 2 (count chrom2)))]}
     (exactly n hasDirectEvent
              (owl-and InverseInsertionTwoChromosome
                      (owl-some hasReceivingBreakPoint (first chrom1))
                      (owl-some hasProvidingBreakPoint
                               (first chrom2)
                               (second chrom2))))))


;; Chromosomal Band Inversion : includes both paracentric (involves
;; only 1 arm) and pericentric (involves both arms) inversion.
;; Involves only 1 chromosome
(defn inversion [n band1 band2]
  "Returns an inversion retriction. N is the number of inversion
restrictions. BAND1, BAND2 is of type HumanChromosomeBand."
  (exactly n hasDirectEvent
           (owl-and Inversion
                   (owl-some hasBreakPoint band1 band2))))

;; Chromosomal Band Quadruplication
;; Note: It is not possible to indicate the orientations of the
;; segments with the short system!
(defn quadruplication [n band1 band2]
  "Returns a quadruplication retriction. N is the number of
quadruplication restrictions. BAND1, BAND2 is of type
HumanChromosomeBand."
  (exactly n hasDirectEvent
           (owl-and Quadruplication
                   (owl-some hasBreakPoint band1 band2))))

;; Auxilary function for translocation function.
(defn- adjust-bands [bands]
  "Returns a vector of vectors - each vector contains 2 bands. BANDS
is a list of vectors - each vector contains 1 or 2 HumanChromosome."
  (into []
        (for [band bands]
          (cond
           ;; If band only contains 1 HumanChromosomeBand then
           ;; identify associated telomere.
           (= (count band) 1)
           (conj band (get-telomere (first band)))
           ;; If band only contains 2 HumanChromosomeBand then
           ;; return band i.e. do nothing.
           (= (count band) 2)
           band
           :default
           (throw (IllegalArgumentException.
                   (str "Band should contain 1 or 2
                   HumanChromosomeBand: " band)))))))

;; Chromosomal Band Translocation
;; Must involve more than one chromosome/band
(defn translocation [n & bands] {:pre (> 1 (count bands))}
  "Returns a translocation restriction. N is the number of
translocation restrictions.BANDS is a list of vectors - each vector
contains 1 or 2 HumanChromosomeBand"
  (let [sorted-bands (adjust-bands bands)]
    (exactly n hasDirectEvent
             (owl-and Translocation
                     (for [x (range (count sorted-bands))]
                       (let [curr-band (get sorted-bands x)]
                         (apply owl-and
                                (owl-some hasReceivingBreakPoint (first curr-band))
                                (owl-some hasReceivingBreakPoint (second curr-band))
                                (if (= x (- (count sorted-bands) 1))
                                  [(owl-some hasProvidingBreakPoint
                                            (first (first sorted-bands)))
                                   (owl-some hasProvidingBreakPoint
                                            (second (first sorted-bands)))]
                                  (let [next-band (get sorted-bands (+ x 1))]
                                    [(owl-some hasProvidingBreakPoint
                                              (first next-band))
                                     (owl-some hasProvidingBreakPoint
                                              (second next-band))])))))))))

;; Chromosomal Band Triplication
;; Invovles only 1 chromosome
;; QUERY: Book says "It is not possible to indicate the orientations
;; of the segments with the short system" however the example shown
;; seem to show the orientations fine. What other detailed systems
;; occur for the first example?  Similar to Duplication
(defn triplication [n band1 band2]
  "Returns a triplication retriction. N is the number of triplication
restrictions. BAND1, BAND2 is of type HumanChromosomeBand."
  (exactly 1 hasDirectEvent
           (owl-and Triplication
                   (owl-some hasBreakPoint band1 band2))))

;; Choromosomal Band DirectTriplication
;; Invovles only 1 chromosome
(defn direct-triplication [n band1 band2]
  "Returns a direct-triplication retriction. N is the number of
triplication restrictions. BAND1, BAND2 is of type
HumanChromosomeBand."
  (exactly 1 hasDirectEvent
           (owl-and DirectTriplication
                   (owl-some hasBreakPoint band1 band2))))

;; Choromosomal Band InverseTriplication
;; Invovles only 1 chromosome
(defn inverse-triplication [n band1 band2]
  "Returns an inverse-triplication retriction. N is the number of
  inverse-triplication restrictions. BAND1, BAND2 is of type
  HumanChromosomeBand."
  (exactly 1 hasDirectEvent
           (owl-and InverseTriplication
                   (owl-some hasBreakPoint band1 band2))))