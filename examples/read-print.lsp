
(define (calculator)
  (let (x (read))
  (let (y (read))
    (print (list (+ x y) (- x y) (* x y) (if (eq? y 0) null (/ x y)))))))

(calculator)

(define (last lst)
    (if (null? (cdr lst))
        (car lst)
        (last (cdr lst))))

(print (last (read)))
