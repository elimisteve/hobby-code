;; Chapter 24 of Information Theory, Inference and Learning Algorithms

;; We have some data which we imagine comes from a gaussian distribution.
;; We're trying to calculate various best guesses for mu and sigma 

;; First we need a definition of the gaussian distribution function:
(defn gaussian [mu sigma]
  (let [factor (/ 1 (Math/sqrt (* 2 Math/PI)) sigma)]
    (fn [x]
      (let [ssd (/ (- x mu) sigma)
            sd (* ssd ssd)]
        (* factor (Math/exp (* -1/2 sd)))))))

;; And a method of integration. This is the parallelogram rule, which calculates the area of
;; a load of parallelograms fitted under the curve
(defn integrate [f a b N]
  (let [h (/ (- b a) N)
        points (map #(+ a (* % h)) (range (inc N)))
        vals (map f points)
        weights (concat [1] (repeat (dec N) 2) [1])
        weighted-vals (map * weights vals)]
    (* (/ h 2) (reduce + weighted-vals))))

;; integral of x over 0 1 is 1/2. Parallelogram rule should find that exactly.
(integrate identity 0 1 1) ; 1/2
(integrate identity 0 1 10) ; 1/2
;; of x^2 is 1/3, Should be an h^2 approximation
(integrate #(* % %) 0 1 1) ; 1/2
(integrate #(* % %) 0 1 10) ; 67/200
(integrate #(* % %) 0 1 100) ; 6667/20000
(integrate #(* % %) 0 1 1000) ; 666667/2000000
(integrate #(* % %) 0 1 10000) ; 66666667/200000000
(integrate #(* % %) 0 1 10000.0)    ; 0.33333333499999834
(integrate #(* % %) 0 1 100000.0)   ; 0.3333333333499996
(integrate #(* % %) 0 1 1000000.0)  ; 0.3333333333335003


;; sanity checks. A gaussian with mean 0 and standard deviation 1 should have a peak at 0
(map (gaussian 0 1) [-0.01 0 0.01]) ; (0.39892233378608216 0.3989422804014327 0.39892233378608216)
;; should have a total probability mass of 1
(integrate (gaussian 0 1) -100 100 1000) ; 1.0000000000000004
;; and the majority of its mass should be between -1 and 1
(integrate (gaussian 0 1) -1 1 1000) ; 0.6826893308232487
;; 95 % of the probability mass should within 2 standard deviations
(integrate (gaussian 0 1) -2 2 1000) ; 0.9544994481518969
;; Similarly, with mean 10 and sd 5
(integrate (gaussian 10 5) -100 100 1000) ; 1.0
(integrate (gaussian 10 5) 5 15 1000) ; 0.6826893308232493
(integrate (gaussian 10 5) 0 20 1000) ; 0.9544994481518962

;; Given our gaussian function, we can calculate the probability of a data set
;; xs being generated by a gaussian of mean mu and sd sigma, which, when
;; considered as a function of mu and sigma for a fixed data set, is known as
;; the likelihood
(defn likelihood [xs mu sigma]
  (reduce * (map (gaussian mu sigma) xs)))

;; One classical way to estimate the parameters mu and sigma is to take the
;; parameters which maximize the likelihood.
;; Suppose our data is [-1 0 1]
;; This is how to find the maximum by hand:
(likelihood [-1 0 1] 0 1) ; 0.02335800330543158
(likelihood [-1 0 1] 0 2) ; 0.006181111673204696
(likelihood [-1 0 1] 0 0.1) ; 2.362011496691831E-42
(likelihood [-1 0 1] 0 0.4) ; 0.001915180501771747
(likelihood [-1 0 1] 0 0.5) ; 0.009303412060034514
(likelihood [-1 0 1] 0 0.6) ; 0.018276914721837227
(likelihood [-1 0 1] 0 0.7) ; 0.024050317175942977
(likelihood [-1 0 1] 0 0.8) ; 0.025994119342662193
(likelihood [-1 0 1] 0 0.9) ; 0.025341752327009883
(likelihood [-1 0 1] 0 0.81) ; 0.026022066004237898
(likelihood [-1 0 1] 0 0.82) ; 0.02602564769190768
(likelihood [-1 0 1] 0 0.83) ; 0.026006302784975496
(likelihood [-1 0 1] 0 0.811) ; 0.02602349662698512
(likelihood [-1 0 1] 0 0.812) ; 0.026024685075132118
(likelihood [-1 0 1] 0 0.813) ; 0.026025632798181965
(likelihood [-1 0 1] 0 0.814) ; 0.02602634124274292
(likelihood [-1 0 1] 0 0.815) ; 0.026026811852466962
(likelihood [-1 0 1] 0 0.816) ; 0.026027046067989387
(likelihood [-1 0 1] 0 0.817) ; 0.026027045326869728

;; Looks like the likelihood is maximized at mu = 0 and sigma = 0.816

;; However, it's actually possible to directly calculate the maximum likelihood estimator.
;; We set sigma against the average of the deviations from the mean squared
;; we want mu=0 and sigma = sqrt(2/3)
(Math/sqrt 2/3) ; 0.816496580927726

;; Which convinces me that my gaussian formula's right!

;; sanity checks for the likelihood function
;; likelihood of a gaussian with sd sigma hitting its mean n times should be
;; (in latex notation) {\sqrt{(1/2pi)}\sigma}^n
(/ (Math/pow (Math/sqrt (* Math/PI 2)) 3)) ; 0.06349363593424098
(likelihood [0 0 0] 0 1) ; 0.06349363593424098
(likelihood [100 100 100] 100 1) ; 0.06349363593424098

;; As sigma reduces, it should blow up violently
(likelihood [0 0 0] 0 1) ; 0.06349363593424098
(likelihood [0 0 0] 0 0.1) ; 63.49363593424098
(likelihood [0 0 0] 0 0.01) ; 63493.63593424098

;; More violently as the number of data points increases
(likelihood [0 0 0 0 0] 0 1) ; 0.010105326013811646
(likelihood [0 0 0 0 0] 0 0.1) ; 1010.5326013811645
(likelihood [0 0 0 0 0] 0 0.01) ; 1.0105326013811643E8

;; The blow up should only happen for the correct mean!
(likelihood [0 0 0] 0.01 1) ; 0.06348411260311854
(likelihood [0 0 0] 0.01 0.1) ; 62.54833884763085
(likelihood [0 0 0] 0.01 0.01) ; 14167.345154413284
(likelihood [0 0 0] 0.01 0.005) ; 1259.0799062116896
(likelihood [0 0 0] 0.01 0.0025) ; 1.534060917486296E-4
(likelihood [0 0 0] 0.01 0.001) ; 4.555729315133397E-58


;; but if the data points are spread out
;; then the maximum likelihood should be at their mean for any given sigma
(likelihood [1 2 3 4 5] 2.9 1) ; 6.640802395922553E-5
(likelihood [1 2 3 4 5] 3 1) ; 6.808915108954251E-5
(likelihood [1 2 3 4 5] 3.1 1) ; 6.640802395922553E-5

(defn sample-mean [xs]
  (/ (reduce + xs) (count xs)))

(sample-mean [1 2 3 4 5]) ; 3

(defn sample-variance [xs]
  (sample-mean
   (map #(* % %)
        (map #(- % (sample-mean xs)) xs))))

(sample-variance [1 2 3 4 5]) ; 2

;; and the global maximum likelihood should have mean equal to the sample mean
;; and with a standard deviation which is the square root of the sample variance
(Math/sqrt (sample-variance [1 2 3 4 5])) ; 1.4142135623730951
(likelihood [1 2 3 4 5] 3 1.4142) ; 1.4663550358059296E-4

;; This is good evidence that that's true:
(likelihood [1 2 3 4 5] 2.99 1.4142) ; 1.466171749366661E-4
(likelihood [1 2 3 4 5] 3.01 1.4142) ; 1.466171749366661E-4
(likelihood [1 2 3 4 5] 3 1.40) ; 1.4656020514688226E-4
(likelihood [1 2 3 4 5] 3 1.42) ; 1.4662331295862982E-4

;; So far, this is classical statistics.
;; We've calculated the maximum likelihood estimator for a two parameter distribution

;; Note that this is actually a biased estimator.

;; Suppose that there were a million parallel universes.
;; In each one, the probablility distribution was (gaussian 0 1)
;; And in each universe, 5 samples were drawn

;; How do we make gaussian samples, anyway?
;; This is one popular way!
(defn g-sample[mu sigma]
  (+ mu
     (* sigma
        (Math/sin (rand (* 2 Math/PI)))
        (Math/sqrt (* -2 (Math/log (rand)))))))

;; here are a load of different sets of 5 samples from the same gaussian
(def g-samples (partition 5 (repeatedly #(g-sample 0 1))))

;; If the guys from the different universes add up their estimates for the mean, they can
;; eventually collaborate to get the right answer.
;; The average of the estimators for the mean tends (slowly) to zero as more and more results
;; are added in

(defn running-average [sq]
  (map /
     (reductions + sq)
     (iterate inc 1)))

(running-average (map sample-mean g-samples))

;; But their estimates for the variance don't tend to one.
(running-average (map sample-variance g-samples))

;; We can also construct the unbiased estimator for the variance
(defn frigged-sample-variance [xs]
  (/ (reduce + 
             (map #(* % %)
                  (map #(- % (sample-mean xs)) xs)))
     (dec (count xs))))

;; Which does actually tend to the true variance
(running-average (map frigged-sample-variance g-samples))
;; Note to the variance, which is the standard deviation squared
(map #(Math/sqrt %)
     (drop 5000 (running-average
            (map frigged-sample-variance
                 (partition 5 (repeatedly #(g-sample -1 2)))))))
;; The average square roots don't head for the right answer
(drop 1000 (running-average
            (map #(Math/sqrt %) (map frigged-sample-variance
                                     (partition 5 (repeatedly #(g-sample -1 2)))))))

;; What about the a priori?

;; An improper prior flat in mu and in log sigma
(defn scaly-prior [mu sigma] (/ sigma))
;; And here's a flat prior
(defn flat-prior [mu sigma] 1)

;; Neither of these priors are integrable! We need to understand the calculations below as
;; some sort of limiting procedure!

(defn apriori [xs mu sigma prior]
  (* (likelihood xs mu sigma) (prior mu sigma)))

;; to find P(xs|sigma) we want to marginalize over mu

;; Approximate the integral over all mu with
(defn marginal [xs sigma prior]
  (integrate #(apriori xs % sigma prior) -100 100 1000)))

(marginal [-1 0 1] 0.1 flat-prior) ; 4.747442358295283E-43
(marginal [-1 0 1] 0.5 flat-prior) ; 0.006731960638313491
(marginal [-1 0 1] 0.4 flat-prior) ; 0.0011086616110382909
(marginal [-1 0 1] 0.6 flat-prior) ; 0.015870238106611062
(marginal [-1 0 1] 0.7 flat-prior) ; 0.024363975553451225
(marginal [-1 0 1] 0.79 flat-prior) ; 0.029657332974866286
(marginal [-1 0 1] 0.8 flat-prior) ; 0.030095003787980325
(marginal [-1 0 1] 0.81 flat-prior) ; 0.030503951363120632
(marginal [-1 0 1] 0.815 flat-prior) ; 0.03069784494501325
(marginal [-1 0 1] 0.82 flat-prior) ; 0.030884793765711644
(marginal [-1 0 1] 0.83 flat-prior) ; 0.031238200913906517
(marginal [-1 0 1] 0.9 flat-prior) ; 0.03300718279775266
(marginal [-1 0 1] 0.99 flat-prior) ; 0.03379688671890863
(marginal [-1 0 1] 1 flat-prior) ; 0.033803760991572916
(marginal [-1 0 1] 1.01 flat-prior) ; 0.03379711207009581
(marginal [-1 0 1] 2 flat-prior) ; 0.017890640645182674
(marginal [-1 0 1] 4 flat-prior) ; 0.0053950579819349595
(marginal [-1 0 1] 8 flat-prior) ; 0.0014134930551064637
(marginal [-1 0 1] 16 flat-prior) ; 3.5753871598834274E-4

;; With a flat prior, say our adversary is choosing sigma by randomly rolling a d1000000 and dividing by 1000000, three points is just enough to give sigma a maximum at 1,
;; I calculate it should be at sqrt (2), and that the die off of this function should be 1/sigma
;; meaning that although the posterior has a mode, its mean or median are determined by
;; the adversary's range.

(marginal [-1 0 1] 0.1 scaly-prior) ; 4.747442358295282E-42
(marginal [-1 0 1] 0.5 scaly-prior) ; 0.013463921276626982
(marginal [-1 0 1] 0.4 scaly-prior) ; 0.0027716540275957266
(marginal [-1 0 1] 0.6 scaly-prior) ; 0.02645039684435177
(marginal [-1 0 1] 0.7 scaly-prior) ; 0.034805679362073176
(marginal [-1 0 1] 0.79 scaly-prior) ; 0.037540927816286436
(marginal [-1 0 1] 0.8 scaly-prior) ; 0.03761875473497538
(marginal [-1 0 1] 0.81 scaly-prior) ; 0.03765919921372918
(marginal [-1 0 1] 0.815 scaly-prior) ; 0.037666067417194166
(marginal [-1 0 1] 0.82 scaly-prior) ; 0.03766438264111176
(marginal [-1 0 1] 0.83 scaly-prior) ; 0.03763638664326088
(marginal [-1 0 1] 0.9 scaly-prior) ; 0.03667464755305852
(marginal [-1 0 1] 0.99 scaly-prior) ; 0.03413826941303902
(marginal [-1 0 1] 1 scaly-prior) ; 0.033803760991572916
(marginal [-1 0 1] 1.01 scaly-prior) ; 0.03346248719811465
(marginal [-1 0 1] 2 scaly-prior) ; 0.008945320322591337
(marginal [-1 0 1] 4 scaly-prior) ; 0.0013487644954837399
(marginal [-1 0 1] 8 scaly-prior) ; 1.7668663188830796E-4
(marginal [-1 0 1] 16 scaly-prior) ; 2.234616974927142E-5

;; With a scaly prior, it looks as though the best estimate for sigma is 0.81
;; again, which it really shouldn't be!, and as though the prior dies off as 1/sigma^3

