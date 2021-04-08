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

#define CHUCKSIZE 100

//const int map4[4]={3,2,1,0};

int64_t now()
{
  struct timeval tv;
  gettimeofday(&tv, NULL);
  return tv.tv_sec * 1000000 + tv.tv_usec;
}

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
    //printf("solving %d\n",start+i);
    input(block[start + i]);
    init_cache();
    //printf("%d:",start+i);
    block_solved[start + i] = solve(0);
    if (block_solved[start + i])
    {
      //printf("%d:%s",start+i,block[start+i]);
    }
  }
  mtx.unlock();
}

int main(int argc, char *argv[])
{
  //CHECKED IN REAL MACHINES THAT 4 CORES OF CPU ARE USED IN MY COMPUTER
  const int thread_num = 3;
  std::thread ths[thread_num];
  init_neighbors();

  FILE *fp = fopen(argv[1], "r");
  char puzzle[128];
  block = new char *[CHUCKSIZE];
  block_solved = new bool[CHUCKSIZE];
  for (int i = 0; i < CHUCKSIZE; i++)
  {
    block[i] = new char[N]; //initialize the array
  }

  int64_t start = now();

  bool eof_flag = false;

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
        //this is a valid sudoku
        memcpy(block[total % CHUCKSIZE], puzzle, strlen(puzzle));
        //printf("%d:%s",total,block[total%100]);
        ++total;
        ++batch_cnt;
      }
    } while (batch_cnt < CHUCKSIZE);

    //step size that indicates how many sudokus that a single thread should solve.
    int step = (batch_cnt + thread_num - 1) / 4; //to make sure each step is bigger than 1.
    int offset = 0;

    for (int i = 0; i < thread_num; ++i)
    {
      //printf("%d,%d,%d\n",i,offset,(offset+step>=batch_cnt)?(batch_cnt-offset):step);
      ths[i] = std::thread(block_solve, offset, (offset + step >= batch_cnt) ? (batch_cnt - offset) : step);
      //ths[i].join();
      offset += step;
    }
    // update by zxc 
    for (int i = 0; i < thread_num; i++)
    {
      ths[i].join();
    }
    total_solved += batch_cnt;
  }

  int64_t end = now();
  double sec = (end - start) / 1000000.0;
  printf("%f sec %f ms each %d\n", sec, 1000 * sec / total, total_solved);

  return 0;
}


