
(define (my-split lst)
  (if (null? lst)
      (cons null null)
      (if (null? (cdr lst))
          (cons lst null)
          (let (prev (my-split (cdr lst)))
            (cons (cons (car lst) (cdr prev)) (car prev))))))

(define (merge-list lst1 lst2)
  (if (null? lst1) lst2
      (if (null? lst2) lst1
          (let (x1 (car lst1))
          (let (x2 (car lst2))
            (if (< x1 x2)
                (cons x1 (merge-list (cdr lst1) lst2))
                (cons x2 (merge-list lst1 (cdr lst2)))
                ))))))

(define (merge-sort lst)
  (if (null? lst) lst
      (if (null? (cdr lst)) lst
          (let (pair (my-split lst))
            (merge-list (merge-sort (car pair)) (merge-sort (cdr pair)))))))

(merge-sort '())
(merge-sort '(1 2 3 4 5))
(merge-sort '(5 4 3 2 1))
(merge-sort '(69 420 42))

