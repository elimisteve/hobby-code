;; Eleven urns, all have ten balls. Urn zero has all black balls, Urn one has one white and the rest black, etc.

;; What are the odds?
(reduce +
        (map *
             '(1 10 45 120 210 252 210 120 45 10 1)
             (reductions * 1 (repeat 10 8))
             (reverse (reductions * 1 (repeat 10 2)))))


(defn urn-10-odds [n]
        (map *
             '(1 10 45 120 210 252 210 120 45 10 1)
             (reductions * 1 (repeat 10 (- 10 n)))
             (reverse (reductions * 1 (repeat 10 n)))))

(def urn10-odds-table (map urn-10-odds (range 11)))

((0 0 0 0 0 0 0 0 0 0 10000000000)
 (1 90 3645 87480 1377810 14880348 111602610 573956280 1937102445 3874204890 3486784401)
 (1024 40960 737280 7864320 55050240 264241152 880803840 2013265920 3019898880 2684354560 1073741824)
 (59049 1377810 14467005 90016920 367569090 1029193452 2001209490 2668279320 2334744405 1210608210 282475249)
 (1048576 15728640 106168320 424673280 1114767360 2006581248 2508226560 2149908480 1209323520 403107840 60466176)
 (9765625 97656250 439453125 1171875000 2050781250 2460937500 2050781250 1171875000 439453125 97656250 9765625)
 (60466176 403107840 1209323520 2149908480 2508226560 2006581248 1114767360 424673280 106168320 15728640 1048576)
 (282475249 1210608210 2334744405 2668279320 2001209490 1029193452 367569090 90016920 14467005 1377810 59049)
 (1073741824 2684354560 3019898880 2013265920 880803840 264241152 55050240 7864320 737280 40960 1024)
 (3486784401 3874204890 1937102445 573956280 111602610 14880348 1377810 87480 3645 90 1)
 (10000000000 0 0 0 0 0 0 0 0 0 0))

(defn multipliers [n]
  (map #(nth % n) urn10-odds-table))

(map #(Math/log10 %) (multipliers 5))
(multipliers 5)

(defn odds-to-probs [sq]
  (let [s (reduce + sq)] (map #(/ % s) sq)))

(map float (odds-to-probs (multipliers 3)))

(map #(Math/log10 %) (odds-to-probs (multipliers 3)))
