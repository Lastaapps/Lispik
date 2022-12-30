
(letrec
    (fact (lambda (n)
        (if (eq? n 0)
            1
            (* n (fact (- n 1)))
        )
    ))
    (cons (fact 0) (cons (fact 1) (cons (fact 2) (cons (fact 3) nil))))
)
