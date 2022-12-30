

(define (my-pow x n)
  (if (eq? n 0)
      1
      (* x (my-pow x (- n 1)))))

(my-pow 2 0)
(my-pow 2 1)
(my-pow 2 2)
(my-pow 2 3)
(my-pow 2 4)

(define (my-mod a b)
  (- a (* b (/ a b))))

(my-mod 0 5)
(my-mod 1 5)
(my-mod 2 5)
(my-mod 3 5)
(my-mod 4 5)
(my-mod 5 5)
