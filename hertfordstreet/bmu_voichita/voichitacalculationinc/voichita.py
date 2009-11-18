""" This module implements the Wavelet-Maximum Likelihood Algorithm
developed by Voichita Maxim for the Brain Mapping Unit at Cambridge
University.

This assumes that the input was generated by a fractional Gaussian
noise process, a statistically self similar long-memory noise
characterised by parameters H, the Hurst exponent and sigma, the
variance.

It uses the wavelet transform to calculate a 'power spectrum' which is
then compared using an iterative algorithm to the calculated 'power
spectra'of the Gaussian noises.

The transform uses Daubechies 4 wavelets, treating the signal as
periodic, which is to say taking the necessary extra data at the
endpoints from the other end of the signal. """


from wavelet import PeriodizedDaubechies4WaveletTransform, dyadlength
from fractionalgaussiannoise import IntegratedSpectralDensityFunction
from math import sin, pi, log, sqrt

#http://aspn.activestate.com/ASPN/Cookbook/Python/Recipe/52201
class MemoizeMutable:
    """Memoize(fn) - an instance which acts like fn but memoizes its arguments
       Will work on functions with mutable arguments (slower than Memoize)
    """
    def __init__(self, fn):
        self.fn = fn
        self.memo = {}
    def __call__(self, *args):
        import cPickle
        str = cPickle.dumps(args)
        if not self.memo.has_key(str):
            self.memo[str] = self.fn(*args)
        return self.memo[str]

def WaveletVariances(HurstExponents, waveletsteps, quadraturesteps=20):
    """Given a vector of Hurst Exponents, calculate the expected power spectra
    for each.
    """
    ScaleVariances=[]
    for H in HurstExponents:
        ScaleVariances.append(IntegratedSpectralDensityFunction(H, waveletsteps, quadraturesteps))
    return ScaleVariances

WaveletVariances=MemoizeMutable(WaveletVariances) #replace this function with a memoised version (it is expensive)

def squaresum(list):
    return sum([x*x for x in list])

def calculateSE(waveletcoefficients, waveletsteps):
    Se=[]
    N = len(waveletcoefficients)
    while(waveletsteps>=1):
        Se = [squaresum(waveletcoefficients[N/2:N])]+Se
        waveletsteps-=1
        N=N/2
    return Se

def innerproduct(a,b):
    return sum([x*y for x,y in zip(a,b)])

def sumaoverb(a,b):
    return sum([float(x)/float(y) for x,y in zip(a,b)])
    
def WaveletMaximumLikelihood_FractionalGaussianNoiseParameterEstimator(x, waveletsteps, HurstExponents, ScaleVariances, I):


    N,J=dyadlength(x)
    j0=J-waveletsteps
    waveletcoefficients=PeriodizedDaubechies4WaveletTransform(x,j0)
    SE = calculateSE(waveletcoefficients, waveletsteps)
    
    mext=[ 2**a for a in range(j0,J+1)]
    m=mext[0:-1]

    TruncatedScaleVariances=[a[1:] for a in ScaleVariances]
    LogTruncatedScaleVariances=[[log(x) for x in a] for a in TruncatedScaleVariances] #straight lines!
    mLogTruncatedScaleVariances=[ innerproduct(m,d) for d in LogTruncatedScaleVariances]
    SeoneoverTruncatedScaleVariances=[ sumaoverb(SE,d) for d in TruncatedScaleVariances]

    
    #plotit(mLogTruncatedScaleVariances, "m.log(ScaleVariances)")
    #plotthem(TruncatedScaleVariances, HurstExponents)
    #plotthem(LogTruncatedScaleVariances, HurstExponents)
    #plotit(SeoneoverTruncatedScaleVariances[:-1], "se.(1/ScaleVariances)")

    sigold2=1e308 #largest float
    TOL=1E-20
    wsize=N-N/(2**waveletsteps)

    
    S=TruncatedScaleVariances[I]

    
    signew2 = sumaoverb(SE,S)/wsize

    it=1


    while abs(sigold2-signew2)>TOL:
        sigold2=signew2
        
        LogLikelihood=[a+float(b)/signew2 for (a,b) in zip(mLogTruncatedScaleVariances,SeoneoverTruncatedScaleVariances)]

        #plotit( LogLikelihood[:-1])

        I=LogLikelihood.index(min(LogLikelihood))

        S=TruncatedScaleVariances[I]
        signew2 = sum([ float(a)/b for a,b in zip(SE,S)])/wsize

        it+=1
        if it>100:
            raise("Convergence Problem")

    return HurstExponents[I], sqrt(signew2), [signew2*x for x in ScaleVariances[I]]

def VoichitaNumber(x, waveletsteps, hsteps, initialguess=0.5):
    HurstExponents=[s/(1.0*hsteps) for s in range(hsteps+1)]
    ScaleVariances=WaveletVariances(HurstExponents, waveletsteps)
    I=int(round(hsteps*initialguess))
    H, sigma, Sb = WaveletMaximumLikelihood_FractionalGaussianNoiseParameterEstimator(x, waveletsteps, HurstExponents, ScaleVariances, I)
    return H, sigma



def _test():
    import doctest
    return doctest.testmod()

def selftest():
    print "doctests ", _test()
    
    print "comparisons with original matlab results"

    tests=[
    (10, 8, 10, burstwiggle, 0.1, 0.8),
    (10, 8, 10, burstwiggle, 0.2, 0.8),
    (10, 8, 10, burstwiggle, 0.3, 0.7),
    (10, 8, 10, burstwiggle, 0.4, 0.7),
    (10, 8, 10, burstwiggle, 0.5, 0.7),
    (10, 8, 10, burstwiggle, 0.6, 0.7),
    (10, 8, 10, burstwiggle, 0.7, 0.7),
    (10, 8, 10, burstwiggle, 0.8, 0.8),
    (10, 8, 10, burstwiggle, 0.9, 0.9),

    (10, 8, 100, burstwiggle, 0.1, 0.85),
    (10, 8, 100, burstwiggle, 0.5, 0.75),
    (10, 8, 100, burstwiggle, 0.9, 0.9),
    
    (8, 4, 100, twinwiggles, 0.1,0.34),
    (8, 4, 100, twinwiggles, 0.2,0.34),
    (8, 4, 100, twinwiggles, 0.3,0.34),
    (8, 4, 100, twinwiggles, 0.4,0.34),
    (8, 4, 100, twinwiggles, 0.5,0.34),
    (8, 4, 100, twinwiggles, 0.6,0.34),
    (8, 4, 100, twinwiggles, 0.7,0.34),
    (8, 4, 100, twinwiggles, 0.8,0.34),
    (8, 4, 100, twinwiggles, 0.9,0.34),
    (8, 4, 100, twinwiggles, 0.91,0.34),
    (8, 4, 100, twinwiggles, 0.92,0.34),
    (8, 4, 100, twinwiggles, 0.93,0.34),
    (8, 4, 100, twinwiggles, 0.94,0.94),
    (8, 4, 100, twinwiggles, 0.95,0.95),
    (8, 4, 100, twinwiggles, 0.96,0.96),
    (8, 4, 100, twinwiggles, 0.97,0.97),
    (8, 4, 100, twinwiggles, 0.98,0.98),
    (8, 4, 100, twinwiggles, 0.99,0.99),

    (10,8,10 ,xsquared, 0.1, 0.9),
    (10,8,10 ,xsquared, 0.9, 0.9),
    (10,8,100,xsquared, 0.5, 0.98),
    (10,8,100,xsquared, 0.1, 0.99),
    (9, 7,10, xsquared, 0.5, 0.9),
    (9, 7,100,xsquared, 0.5, 0.97),
    (9, 6,100,xsquared, 0.5, 0.96),
    (9, 5,100,xsquared, 0.5, 0.95),

    (12, 8,  100, burstwiggle, 0.5, 0.96),
    (11, 8,  100, burstwiggle, 0.5, 0.94),
    (9,  8,  100, burstwiggle, 0.5, 0.0),
    (9,  7,  100, burstwiggle, 0.5, 0.0),
    (9,  6,  100, burstwiggle, 0.5, 0.0),

    (9, 5, 100, sqrtslowingwiggle,0.5,0.95),
    (12,10,100, sqrtslowingwiggle,0.5,0.99),
    (12,5, 100, sqrtslowingwiggle,0.5,0.95),
    (12,3, 100, sqrtslowingwiggle,0.5,0.92),

    (10,6,100, twinwiggles, 0.5,0.94),
    (10,7,100, twinwiggles, 0.5,0.94),
    (10,8,100, twinwiggles, 0.5,0.94),
    (12,8,100, twinwiggles, 0.5,0.97),
    (11,7,100, twinwiggles, 0.5,0.96),
    (10,6,100, twinwiggles, 0.5,0.94),
    (9 ,5,100, twinwiggles, 0.5,0.72),
    (8, 4,100, twinwiggles, 0.5,0.34),
    (7 ,3,100, twinwiggles, 0.5,0.80),
    
    (8, 8,100, twinwiggles, 0.5,0.68),
    (8, 7,100, twinwiggles, 0.5,0.69),
    (8, 6,100, twinwiggles, 0.5,0.69),
    (8, 5,100, twinwiggles, 0.5,0.67),
    (8, 4,100, twinwiggles, 0.5,0.34),
    (8, 3,100, twinwiggles, 0.5,0.01),
    (8, 2,100, twinwiggles, 0.5,0.00),
    ]

    testresults=[(trial(*x),x) for x in tests]
    testsfailed=[x for x in testresults if x[0]==False]
    if testsfailed:
        print "!!!!!!!SELF TESTS FAILED!!!!!!"
        print '\n'.join([ str(x[1]) for x in testsfailed])
    else:
        print "all tests passed"

    print "end of self test"

def burstwiggle(x):
    return x*(1-x)*sin(320*pi*x)

def sqrtslowingwiggle(x):
    return sqrt(x)*sin(1.0/x)

def xsquared(x):
    return x*x

def twinwiggles(x):
    return sin(10*pi*x)+sin(200*pi*x)

def plotthem(functions, HurstExponents):
    from pylab import plot, axis, show, legend, figure, xlabel, ylabel, title
    for i in range(len(HurstExponents)):
        plot(functions[i], label='H=%0.2f' % HurstExponents[i])
    legend()
    show()
        
def plotit(x, label="Your Label Here"):
    from pylab import plot, axis, show, legend, figure, xlabel, ylabel, title
    plot(x)
    title(label)
    show()

def CVoichita(signal, waveletsteps, hsteps, initialguess):
    import bmu
    csignal=bmu.doubleArray(len(signal))
    for i,x in enumerate(signal):
        csignal[i]=x
    H,sigma=bmu.VoichitaNumber(csignal, len(signal), waveletsteps, hsteps, initialguess)
    return H,sigma
    
def trial(J, waveletsteps, hsteps, fn, initialguess, expectedvalue):
    N=2**J
    x = [ fn(x/(1.0*N)) for x in range (1,N+1)   ]

    CV,csigma=CVoichita(x, waveletsteps, hsteps, initialguess)
    V,sigma=VoichitaNumber(x, waveletsteps, hsteps, initialguess)

    #print "J=", J, "N=", N, "waveletsteps=", waveletsteps, "hsteps=", hsteps, "fn=", fn, "initialguess=", initialguess,
    print "V=", V, "CV=",CV, "( expected:", expectedvalue,")"
    
    if(V==expectedvalue==CV):
        return True
    else:
        print "****FAILED****"
        return False    

if __name__=='__main__':
    selftest()
    trial(10,8,100, burstwiggle, 0.99, 0.99)

    
    
