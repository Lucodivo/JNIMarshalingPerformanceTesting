package com.inasweaterpoorlyknit.jniplayground

fun IntArray.rotateRight(rotateCount: Int){
    val rotateInBounds = (rotateCount % size);
    if(rotateInBounds == 0){ return; }

    // reverse entire int array
    // [1, 2, 3, 4, 5] -> [5, 4, 3, 2, 1]
    var left = 0; var right = size - 1;
    var tmp = 0
    while(left < right){
        tmp = this[left];
        this[left++] = this[right];
        this[right--] = tmp;
    }

    // reverse both sides with rotateCount as pivot (pivot index is included as part of the right side)
    // rotate right 3: [5, 4, 3, {2} , 1] -> [3, 4, 5, 1, 2]
    left = 0; right = rotateInBounds - 1;
    while(left < right){
        tmp = this[left];
        this[left++] = this[right];
        this[right--] = tmp;
    }
    left = rotateInBounds; right = size - 1;
    while(left < right){
        tmp = this[left];
        this[left++] = this[right];
        this[right--] = tmp;
    }
}