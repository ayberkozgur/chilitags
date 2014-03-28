#include <chilitags.hpp>

#include <opencv2/opencv.hpp> // threshold
#include <opencv2/highgui/highgui.hpp> // imread

#include <iostream>
using std::cout;


int main(int argc, char* argv[])
{
    cv::Mat image;
    cv::Mat edges;
    bool inputAreEdges = false;

    if (argc < 2) {
        cout
            << "Usage: chilitags-detect [--edges] <image> [<edges image>]\n\n"
            << "Returns the list of detected tag id's in the image, one per line.\n";
        return 1;
    }
    if (argc == 4 & strcmp(argv[1], "--edges") == 0) {
        image = cv::imread(argv[2]);
        edges = cv::imread(argv[3], CV_LOAD_IMAGE_GRAYSCALE);
        //cv::threshold(edges, edges, 100, 255, cv::THRESH_BINARY);
        inputAreEdges = true;
    }
    else {
        image = cv::imread(argv[1]);
    }

    if(image.data) {
        if (inputAreEdges) {
            for (const auto &tag : chilitags::Chilitags().findFromEdges(image, edges))
                cout << tag.first << "\n";
        }
        else
        {
            for (const auto &tag : chilitags::Chilitags().find(image))
                cout << tag.first << "\n";
        }
        return 0;
    }
    return 1;
}
