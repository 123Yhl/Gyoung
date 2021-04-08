#include <assert.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/time.h>
#include <thread>
#include <pthread.h>
#include <unistd.h>
#include <mutex>
#include "sudoku.h"
#define chucknum 100

//以下下用到的所有的变量
int total_solved = 0;
int total = 0;
std::mutex mtx;
bool (*solve)(int) = solve_sudoku_dancing_links;
char **block;
bool *block_solved;



void *block_solve(int start, int index)
{
  mtx.lock();
  for (int i = 0; i < index; ++i)
  {
    input(block[start + i]);
    init_cache();
    block_solved[start + i] = solve(0);
  }
  mtx.unlock();
}




int64_t now()
{
  struct timeval tv;
  gettimeofday(&tv, NULL);
  return tv.tv_sec * 1000000 + tv.tv_usec;
}



int main(int argc, char *argv[])
{
  const int thread_num = 3;
  std::thread ths[thread_num];
  init_neighbors();
  FILE *fp = fopen(argv[1], "r");
  char puzzle[128];
  block = new char *[chucknum];
  block_solved = new bool[chucknum];

  for (int i = 0; i < chucknum; i++)
  {
    block[i] = new char[N];
  }

  int64_t start = now();
  bool eof_flag = false;
  //以下将所有的if都改成while，符合并发要求
  while (!eof_flag)
  {
    int batch_cnt = 0;
    do
    {
      char *curr_get = fgets(puzzle, sizeof(puzzle), fp);
      if (curr_get == NULL)
      {
        eof_flag = true;
        break;
      }
      if (strlen(puzzle) >= N)
      {
        memcpy(block[total % chucknum], puzzle, strlen(puzzle));
        ++total;
        ++batch_cnt;
      }
    } while (batch_cnt < chucknum);
    int step = (batch_cnt + thread_num - 1) / 4;
    int offset = 0;

    for (int i = 0; i < thread_num; ++i)
    {
      ths[i] = std::thread(block_solve, offset, (offset + step >= batch_cnt) ? (batch_cnt - offset) : step);
      //ths[i].join();
      offset += step;
    }
    for (int i = 0; i < thread_num; i++)
    {
      ths[i].join();
    }
    total_solved += batch_cnt;
  }

  int64_t end = now();
  double sec = (end - start) / 1000000.0;
  printf("%f sec %f ms each %d\n", sec, 1000*sec/total, total_solved);


  return 0;
}
