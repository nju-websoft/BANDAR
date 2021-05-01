#!/bin/bash
echo -e "Welcome to BANDAR: Benchmarking Snippet Generation Algorithms for Dataset Search\n";
echo 'Steps to use this benchmark: ';
echo '0. Prepare the RDF dataset as an N-triple file. (e.g., dataset.nt)';
echo '1. To run preprocess for snippet generation, enter -p `dataset path`. (e.g., -p ./dataset.nt)';
echo '2. To generate a snippet, enter -g `dataset path` `-method` `keyword1,keyword2,...`(if any)';
echo '    `-method`: `-illusnip` for IlluSnip; `-ksd` for KSD; `-tac` for TA+C; `-dualces` for DualCES; `-pruneddp` for PrunedDP++';
echo '    (e.g., -g ./dataset.nt -illusnip    or -g ./dataset.nt -ksd London,Berlin,Europe)'
echo '3. To evaluate a snippet result, enter -e `dataset path` `snippet path` `keyword1,keyword2,...` `queryword1,queryword2,...`';
echo '    (e.g., -e ./dataset,nt ./illusnip-record.txt London,Berlin.Europe London,Berlin,Europe)'
echo '4. To translate a snippet record to triples, enter -t `dataset path` `snippet path`. (e.g., -t ./dataset,nt ./illusnip-record.txt)';
echo -e "\nEnter \"quit\" to quit.\n";
while true
do
    echo -n "Enter your command for preprocess, generate, evaluate or translate a snippet: ";
    read param;
    if [ "$param" == "quit" ]
    then
        break;
    fi
    java -jar -Xms10g -Xmx100g bandar.jar "$param" ;
    echo -e "\n";
done
