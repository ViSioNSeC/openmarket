CREATE VIEW time_span_view AS 
SELECT time_span.id, 
min || "-" || max || " " || name as time_span_string 
FROM time_span 
left join time_type on time_span.time_type_id = time_type.id
;

CREATE VIEW product_view AS 
SELECT product.id,
seller_id,
seller_name,
category_id,
buy,
auction,
quantity,
title,
processing_time_span_id,
time_span_string,
price,
native_currency_id,
currency.iso as price_iso,
variable_price,
price_select,
price_1,
price_2,
price_3,
price_4,
price_5,
expire_time,
start_amount,
reserve_amount,
currency_id as auction_currency_id,
currency2.iso as auction_currency_iso,
shipping.id as shipping_id, 
from_country_id, 
product_html 
FROM product 
left join time_span_view on product.processing_time_span_id = time_span_view.id 
left join product_page on product.id = product_page.product_id 
left join product_price on product.id = product_price.product_id 
left join auction on product.id = auction.product_id 
left join currency on product_price.native_currency_id = currency.id 
left join currency as currency2 on auction.currency_id = currency2.id 
left join shipping on product.id = shipping.product_id 
left join country on shipping.from_country_id = country.id 
;

CREATE VIEW product_thumbnail_view AS 
SELECT 
product.id as product_id,
title,
seller_id,
shop_name, 
count(review.id) as number_of_reviews,
auction,
CASE WHEN auction='1'
	THEN (select max(bid.amount) from bid where bid.auction_id = auction.id) 
	ELSE max(price) 
END as price, 
currency.iso as price_iso 
FROM product 
inner join currency on product_price.native_currency_id = currency.id 
left join seller on product.seller_id = seller.id 
left join auction on product.id = auction.product_id 
left join review on review.product_id = product.id 
left join product_price on product_price.product_id = product.id 
group by product.id
;


CREATE VIEW category_tree_view AS  
SELECT t1.name AS name_1, t1.id AS id_1,
t2.name AS name_2, t2.id AS id_2,
t3.name AS name_3, t3.id AS id_3,
t4.name AS name_4, t4.id AS id_4,
t5.name AS name_5, t5.id AS id_5,
t6.name AS name_6, t6.id AS id_6,
t7.name AS name_7, t7.id AS id_7 
FROM category AS t1 
LEFT JOIN category AS t2 ON t2.parent = t1.id 
LEFT JOIN category AS t3 ON t3.parent = t2.id 
LEFT JOIN category AS t4 ON t4.parent = t3.id 
LEFT JOIN category AS t5 ON t5.parent = t4.id 
LEFT JOIN category AS t6 ON t6.parent = t5.id 
LEFT JOIN category AS t7 ON t7.parent = t6.id 
where t1.parent IS NULL 
;