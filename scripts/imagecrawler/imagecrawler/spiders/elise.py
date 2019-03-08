# -*- coding: utf-8 -*-
import scrapy


class Elise(scrapy.Spider):
   name = 'elise'
   allowed_domains = ['example.com']
   queries = ['car', 'animal', 'nature', 'landscape', 'security']
   template = 'https://www.pexels.com/search/{}/?page={}'
   start_urls = []
   for q in queries:
      for i in range(1, 5):
         start_urls.append(template.format(q, i))
   print(start_urls)

   def parse(self, response):
      for img in response.css('img.photo-item__img'):
         yield {
            # 'link': img.css('::attr(src)').extract_first(),
            'image_urls': [img.css('::attr(src)').extract_first()],
         }
