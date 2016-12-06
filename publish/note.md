---
title: My Note
---

Java 代码 & C/C++ 代码，无语言代码（这不是 note，但应当收录在 fulltext 里）

```java
byte    b   =   3;
int     i   =   b;

double  y   =   2.7;
int     x   =   (int)y;
```

```c
if (okay) {
    printf("Hello World\n");
}
```

```cpp
using namespace std;
if (okay) {
    cout << "Hello World" << endl;
}
```

```
http://127.0.0.1:1080/pac?t=20161203153117736
```

`print hello is your name.`{.cpp}

-   [sting 的广播：博士后](https://www.douban.com/people/sting999/status/1918804013/)

    :   现在国内高校都在大肆扩张博士后或者“师资”博士后，成百上千的招博士后，实
        际上是变相的将青年博士奴工化的方式，能够做完博士后留下成为正式教师的人
        数非常的少，而且越来越少。只要打听一下该校的教师编制和正在招的博士后人
        数的比例就不难看出来。即将找工作的同学们慎之。

        现博士毕业找到固定职位确实非常难了，有学校广撒网招收博后（如广东某校），
        有些学校广撒网招专职科研岗（如南京某校），往往给的待遇确实很高，但是基
        本大部分都很难留下，一拿到面上基金的师姐也忧心忡忡担心能不能留下，基本
        上博士都被当论文狗来使用，好多人博后后者专职留不下，然后悲催人生。

-   网络设置

    :   url: `ms-settings:network-proxy`{.html}

        500 Internal Privoxy Error

        :   Privoxy encountered an error while processing your request:

            Could not load template file no-server-data or one of its included components.

            Please contact your proxy administrator.

            If you are the proxy administrator, please put the required file(s)in the
            (confdir)/templates directory. The location of the (confdir) directory is
            specified in the main Privoxy config file. (It's typically the Privoxy
            install directory).

-   [如何评价音乐剧《汉密尔顿》(Hamilton)？ - 知乎](https://www.zhihu.com/question/36505902)

~~~~ {#mycode1 .haskell .numberLines startFrom="100"}
qsort []     = []
qsort (x:xs) = qsort (filter (< x) xs) ++ [x] ++
               qsort (filter (>= x) xs)
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

~~~~ {.haskell #mycode2 .haskell}
qsort []     = []
qsort (x:xs) = qsort (filter (< x) xs) ++ [x] ++
               qsort (filter (>= x) xs)
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

-   ambiguity: declaration or multiplication? -<

    :   ```cpp
        Class::X *p;
        // X can be object of Class, or a nested class.
        // so ambi'guity occurs
        Type::NestedType   * p;         // declare
        Type::object * p;               // multiplication
        ```

        通常在用 template 的时候可能编译器无法知道到底是何种情况，
        可以加上 `typename` 即用 `typename Type::NestedType *p`。

-   2d-vector is esier to use -<

    :   ```cpp
        // c
        int **ary = new int*[row_num];
        for(int i = 0; i < row_num; ++i) {
            ary[i] = new int[col_num];
            // delete[] ary[i];
        }
        // delete[] ary;

        // cpp
        vector<vector<int> > ary(row_num, vector<int>(col_num, 0));
        ```

        make sure to clear it's contents when necessary.

        ```cpp
        vec.clear();                // the performance depends on how's your dtor

        // or
        vector<T>().swap( x );      // clear x reallocating
        ```

        refs and see also

        -   [vector::clear - C++ Reference](http://www.cplusplus.com/reference/vector/vector/clear/)
        -   [performance - C++ fastest way to clear or erase a vector - Stack Overflow](http://stackoverflow.com/questions/16420357/c-fastest-way-to-clear-or-erase-a-vector)

-   使用 reserve 来避免不必要的重新分配 -<

    :   ```cpp
        // void std::vector::reserve( size_type new_cap );
        vector<int> nums;
        nums.reserve( 25 );
        ```

        但不要以为 `size()` 也变了。你可以用 `resize( int num )`。

        refs and see also

        -   [std::vector::reserve - cppreference.com](http://en.cppreference.com/w/cpp/container/vector/reserve)

-   tolower, toupper, isalpha -<

    :   defined in `<ctype.h>` or `<cctype>` (`std::tolower`).

        **tolower, touppper**

        ```cpp
        #include <ctype.h>

        int toupper( int c );
        int tolower( int c );
        ```

        converts the letter c to upper/lower case, if possible.

        if not ASCII, or EOF, the behavior is undefined.

        **isalpha, isspace, isdigit, isalnum, isxdigit**

        ```cpp
        int isalnum(    int c   );
        int isalpha(    int c   );
        int isascii(    int c   );
        int isblank(    int c   );
        int iscntrl(    int c   );
        int isdigit(    int c   );
        int isgraph(    int c   );
        int isprint(    int c   );
        int ispunct(    int c   );
        int isspace(    int c   );
        int isxdigit(   int c   );
        ```

-   even *vs* odd，奇偶数判断 -<

    :   ```cpp
        // even
        x % 2
        // odd
        x % 2 != 0
        x & 0x1
        ```

-   equal? float/double 数的判等 -<

    :   ```cpp
        // for int
        a == b
        // for double
        fabs(a-b) < 1e-9        // math.h
        ```

-   文件读写，用 FILE，fscanf 或者 ifstream，getline -<

    :   ```cpp
        #include <stdio.h>

        FILE *fp = fopen(filename, "r");
        while( 2 == fscanf( fp, "%d %s", &index, buf ) ) {
            // ...
        }
        fclose(fp);

        size_t fread(        void *ptr, size_t size, size_t nmemb, FILE *stream );
        size_t fwrite( const void *ptr, size_t size, size_t nmemb, FILE *stream );
        ```

        虽然你可能喜欢 `fopen`（就跟我以前一样），但我推荐用 C++ 的 stream，因为
        它更安全（不是指针，没有泄露危险）。

        ```cpp
        #include <fstream>

        std::ifstream file( filename.c_str(), ifstream::in );
        // 或者：std::ifstream file; file.open( filename.c_str(), ifstream::in );

        if ( !file ) { exit(-1); }
        // 或者：if ( !file.is_open() ) { exit(-1); }

        string line;
        while ( getline(file, line) ) {
            // ...
        }
        ```

        这个 `if( !file )` 是因为 file 重载了 `operator void *`，这句话等同于
        `if( NULL == (void *)file )`，见 [ios::operator void* - C++
        Reference](http://program.upc.edu.cn/CLibrary/iostream/ios/operator_voidpt.html)。

        [The Safe Bool Idiom](http://www.artima.com/cppsource/safebool.html) -<

        :   **Learn how to validate objects in a boolean context without the usual harmful side effects.**

            The Goal -<

            :   Test their validity in Boolean contexts

                ```cpp
                // method 1
                if (some_type* p=get_some_type()) {
                    // p is valid, use it
                }
                else {
                    // p is not valid, take proper action
                }

                // method 2
                smart_ptr<some_type> p(get_some_type());
                if (p.is_valid()) {
                    // p is valid, use it
                }
                else {
                    // p is not valid, take proper action
                }
                ```

            The Obvious Approach Is `operator bool`, and also, the Not Exactly Obvious, `operator!` -<

            :   ```cpp
                // operator bool version
                class Testable {
                    bool ok_;
                public:
                    explicit Testable(bool b=true):ok_(b) {}

                    operator bool() const {
                        return ok_;
                    }
                    bool operator!() const {
                        return !ok_;
                    }
                };
                ```

            A Seemingly Innocent Approach: `operator void *` -<

            :   ```cpp
                operator void*() const {
                    return ok_==true ? this : 0;
                }
                ```

                good? see this:

                ```cpp
                Testable test;

                // oops...
                delete test;
                ```

                If you think that this situation can be saved with a little
                const trickery, think again: The C++ Standard explicitly allows
                delete expressions with pointers to const types.

            Almost Getting There with a Nested Class -<

            :   In 1996, Don Box wrote about a very clever technique in his C++
                Report column a technique originally created to support testing
                for nullness that almost does what we came here for. It
                involves a conversion function to a nested type (that doesn't
                even need to be defined), like so:

                ```cpp
                class Testable {
                    bool ok_;
                public:
                    explicit Testable(bool b=true):ok_(b) {}
                    class nested_class;         // no need to implement;
                    operator const nested_class*() const {
                      return ok_ ? reinterpret_cast<const nested_class*>(this) : 0;
                    }
                };
                ```

            The Safe Bool Idiom -<

            :   It's time to make these tests safe. Remember that we need
                to avoid unsafe conversions that allow for erroneous usage.
                We must also avoid overloading issues, and we definitely
                shouldn't allow deletion through the conversion. So, what
                do we do?  Without further ado, let me give you the
                solution in code.

                ```cpp
                class Testable {
                    bool ok_;
                    typedef void (Testable::*bool_type)() const;
                    void this_type_does_not_support_comparisons() const {}
                public:
                    explicit Testable(bool b=true):ok_(b) {}
                    operator bool_type() const {
                        return ok_==true ?  &Testable::this_type_does_not_support_comparisons : 0;
                    }
                };
                ```

            refs and see also

            -   [c++ - Is the safe-bool idiom obsolete in C++11? - Stack Overflow](http://stackoverflow.com/questions/6242768/is-the-safe-bool-idiom-obsolete-in-c11)

`shitshitshit`

```
shitshitshitshitshitshit
```
