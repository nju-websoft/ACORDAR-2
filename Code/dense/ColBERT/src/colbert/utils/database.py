import sys
import pymysql

db1 = pymysql.connect(host='114.212.190.189',
                     port=3306,
                     user='ttlin',
                     password='ttlin',
                     database='dashboard_2021jul')

db2 = pymysql.connect(host='114.212.190.189',
                     port=3306,
                     user='ttlin',
                     password='ttlin',
                     database='dataset_analysis_2021apr')

db3 = pymysql.connect(host='114.212.82.63',
                     port=3306,
                     user='ttlin',
                     password='lintengteng',
                     database='test_collection_2022apr')

# 使用 cursor() 方法创建一个游标对象 cursor
dashboard, analysis, cqs = db1.cursor(), db2.cursor(), db3.cursor()