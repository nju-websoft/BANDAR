# BANDAR
Source codes, results and an example for paper "*[BANDAR: Benchmarking Snippet Generation Algorithms for Dataset Search]()*". 

## Queries and Datasets



## Dependencies

- JDK 8+
- MySQL 8.x
- Apache Lucene 7.5.0
- JGraphT 1.3.0

## Run

To run experiments on the example, please follow these steps: 

1. Import [example.sql]( https://github.com/nju-websoft/BANDAR/blob/master/example.sql ) to your local MySQL database, it contains 5 tables, *triple* and *uri_label_id* store the information of the example dataset, while *dataset_info*, *mrrs* and *snippet* are empty. Open [src]( https://github.com/nju-websoft/BANDAR/tree/master/src ) as a java project, dependency of external jar files are provided in [lib]( https://github.com/nju-websoft/BANDAR/tree/master/lib ). 
2. Configure the information in **util/DBUtil.java** according to your local database settings, namely, *uri*, *name*, *user* and *password*. 
3. Run **snippetGenerationTest/PreprocessOfExample.java**, it will insert records into table *dataset_info* and *mrrs*, create useful indexes for all snippet generation methods. The default path of output indexes is the same as src, you can change it ( if needed ) in **snippetAlgorithm/Parameter.java**. Note that, if you need to rerun the preprocess step, you need to **truncate** table *dataset_info* and *mrrs*, and **delete** the folder of indexes ( dataset1 ) first. 
4. To generate snippets by different methods, all 5 methods are provided as **snippetGenerationTest/xxxTest.java**, namely, *KSD*, *IlluSnip*, *TA+C*, *Dual-CES* and *PrunedDP++*. Directly run the corresponding methods, the result snippet will be presented in the terminal as triples. Besides, you can change the keywords in each main() method. 
5. To get evaluation scores of snippets, run **snippetEvaluation/xxxEvaluation.java** (the snippets need to be generated first as in step 4), the corresponding snippet and metric scores will be presented in the terminal. The evaluation metrics include *SkmRep*, *EntRep*, *DescRep*, *LinkRep*, *KwRel* and *QryRel*. 

> If you have any difficulty or question in running code or reproducing experimental results, please email to [xxwang@smail.nju.edu.cn](mailto:xxwang@smail.nju.edu.cn). 

## Citation

If you use these codes or results, please kindly cite it as follows:

```
@inproceedings{BANDAR,
  author    = {Xiaxia Wang and Gong Cheng and Jeff Z. Pan and Evgeny Kharlamov and Yuzhong Qu},
  title     = {BANDAR: Benchmarking Snippet Generation Algorithms for Dataset Search},
  year      = {2020}
}
```
