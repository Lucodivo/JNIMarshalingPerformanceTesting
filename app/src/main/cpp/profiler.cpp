#include <time.h>

static u64 GetOSTimerFreq(void)
{
  return 1000000;
}

static u64 ReadOSTimer(void)
{
  struct timeval Value;
  gettimeofday(&Value, 0);

  u64 Result = GetOSTimerFreq()*(u64)Value.tv_sec + (u64)Value.tv_usec;
  return Result;
}

inline u64 ReadCPUTimer(void)
{
  const u64 nanosecondsPerSecond = 1000000000;
  timespec t;
  clock_gettime(CLOCK_MONOTONIC, &t);
  return (t.tv_sec * (nanosecondsPerSecond)) + t.tv_nsec;
}

static u64 EstimateCPUTimerFreq(u64 msToWait)
{
  u64 OSFreq = GetOSTimerFreq();

  u64 CPUStart = ReadCPUTimer();
  u64 OSStart = ReadOSTimer();
  u64 OSEnd = 0;
  u64 OSElapsed = 0;
  u64 OSWaitTime = OSFreq * msToWait / 1000;
  while(OSElapsed < OSWaitTime)
  {
    OSEnd = ReadOSTimer();
    OSElapsed = OSEnd - OSStart;
  }

  u64 CPUEnd = ReadCPUTimer();
  u64 CPUElapsed = CPUEnd - CPUStart;

  u64 CPUFreq = 0;
  if(OSElapsed) {
    CPUFreq = OSFreq * CPUElapsed / OSElapsed;
  }

  return CPUFreq;
}