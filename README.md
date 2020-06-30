# BANDAR
Source codes, results and an example for paper "*[BANDAR: Benchmarking Snippet Generation Algorithms for Dataset Search]()*". 

## Queries, Datasets and Results

All queries, datasets and result snippets generated by different methods are provided in [data]( https://github.com/nju-websoft/BANDAR/tree/master/data ). 

- *dataset_id_dump* contains URL links of the dump files for each dataset. The first column is a local id for the dataset which was also used in *query-dataset-pairs*, the second and following columns are links to the dump, all columns are separated by `'\t'`. Note that, one dataset could have more than one dump files. 
- *query-dataset-pairs* contains all pairs used in the snippet generation experiments. It has 4 columns separated by `'\t'`, the first column is the local id of the query-dataset pair, corresponding to the files in *result*, the second column shows the dataset id of the pair, the third column is the original query text, and the fourth column is the content keywords of the query ( which were actually used in snippet generation ).  
- [result](https://github.com/nju-websoft/BANDAR/tree/master/data/result) contains all snippet results. Folder *result/1k*/ contains the snippet results of the *1*-th to *1,000*-th query-dataset pair ( and so on ).  *result/xk/yy/20/* and *result/xk/yy/40/* are of snippet size `k = 20` and `k = 40` respectively. Each snippet is presented as a *.nt* file. 


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
