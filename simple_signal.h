struct sigaction {
  void (*sa_handler)(int);
  int sa_mask;
  int sa_flags;
};

int sigaction(int signum, struct sigaction* act, struct sigaction* oldact);
 
