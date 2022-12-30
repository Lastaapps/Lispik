
(define (fact n)
    (if (eq? n 0)
        1
        (* n (fact (- n 1)))))

(fact 0)
(fact 1)
(fact 2)
(fact 3)
