#!/bin/sh
lang="";
n=1000;
date_archive=$(date -d "$(date +%Y-%m-01) -1 day" +%Y-%m);
sort="desc"
while getopts l:d:n:s: flag
do
    case "${flag}" in
        l) lang=${OPTARG};;
        d) date_archive=${OPTARG};;
        n) n=${OPTARG};;
        s) sort=${OPTARG};;
    esac
done
echo "========================="
echo "lang: $lang";
echo "date: $date_archive";
echo "n: $n";
echo "sort: $sort";
echo "========================="


clickstream_data="clickstream_data_${lang}_${date_archive}"
if [ -f "$clickstream_data" ]
then
  echo "File found"
else
   echo "File not found"
   clickstream_url="https://dumps.wikimedia.org/other/clickstream/";
   content=$(curl -L "$clickstream_url$date_archive/")
   links=$( echo $content | grep -Po '(?<=href=")[^"]*');
   toextract="";
   substr="-${lang}wiki-"
   echo $substr
   for link in ${links[@]}; do
    echo $link
    if [[ $link =~  "-${lang}wiki-" ]];then
        toextract="$clickstream_url$date_archive/$link";
    fi
   done

   if [[ $toextract == "" ]]; then
      echo "Lang not found in clickstream";
      exit 1;
   fi

   echo ">>>> DOWNLOAD $toextract and save it"

  wget -O "${clickstream_data}.gz" $toextract;
  gzip -d "${clickstream_data}.gz"
fi


echo ">>>> COMPUTE SUM OF CLICKS"
declare -A dict
while IFS= read -r line; do
   IFS=$'\t'; arrIN=($line); unset IFS;
   key=${arrIN[1]}
   val=${arrIN[3]}
   if [[ ${key} != *"List"* ]];then
     if [[ ${#dict[${key}]} -eq 0 ]] ;then
       dict[${key}]=$(($val));
     else
       dict[${key}]=$((${dict[${key}]}+$val));
     fi
   fi
done < $clickstream_data

echo ">>>> SORT IT AND SAVE TEMP"
if [[ $sort == "desc" ]]; then
  for page in "${!dict[@]}"
  do
    echo "$page ${dict[$page]}"
  done | sort -rn -k2 | head -n "$n" | cut -d ' ' -f 1 >> temp.txt;
else
    for page in "${!dict[@]}"
    do
      echo "$page ${dict[$page]}"
    done | sort -n -k2 | head -n "$n" | cut -d ' ' -f 1 >> temp.txt;
fi


echo ">>>>> SAVE FINAL FILE : uri_sample_${lang}_${sort}_${n}.lst"
while IFS= read -r line;do
    echo "https://$lang.wikipedia.org/wiki/$line" >> "uri_sample_${lang}_${sort}_${n}.lst"
done < "temp.txt"

rm -rf temp.txt
