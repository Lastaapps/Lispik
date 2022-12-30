
#|
Taken from BI-PPA course solutions at CTU Prague
|#

; ------------------------------------------------------------------------------
; Exercise 1
(define (my-foldl foo init lst)
  (if (null? lst)
      init
      (my-foldl foo (foo init (car lst)) (cdr lst))))

(define (my-foldr foo init lst)
  (if (null? lst)
      init
      (foo (my-foldr foo init (cdr lst)) (car lst))))

; some basic tests
(list
    (my-foldl (lambda (x y) (+ x y)) 0 '(1 2 3))
    (my-foldr (lambda (x y) (* x y)) 1 '(4 3 2 1))
)
(list 6 24)

; ------------------------------------------------------------------------------
; Exercise 2

; 2/1
(define (my-map foo lst)
  (my-foldr (lambda (acc x) (cons (foo x) acc)) null lst)) ; foldl reverzne

; 2/2
(define (my-filter foo lst)
  (my-foldr (lambda (acc x) (if (foo x) (cons x acc) acc)) null lst))

; 2/3
(define (my-append2 lst e)
  (my-foldr (lambda (acc n) (cons n acc)) (cons e null) lst))

; some basic tests
(list
    (my-map (lambda (n) (* n n)) '(1 2 3))
    (my-filter (lambda (n) (and (>= n 3) (<= n 10))) '(1 2 3 4 5 9 10 11))
)
(list '(1 4 9) '(3 4 5 9 10))
